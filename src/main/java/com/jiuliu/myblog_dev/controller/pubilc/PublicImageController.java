/*
 * [PublicImageController.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/4/4
 */

package com.jiuliu.myblog_dev.controller.pubilc;

import com.jiuliu.myblog_dev.config.business.OSSConfig;
import com.jiuliu.myblog_dev.service.oss.ImageService;
import com.jiuliu.myblog_dev.service.oss.ImageService.ImageMeta;
import com.jiuliu.myblog_dev.utils.rateLimit.RateLimit;
import com.jiuliu.myblog_dev.utils.security.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开图片获取接口
 *
 * <p>通过哈希值获取 OSS 中的图片，直接流式传输到客户端。
 * 支持图片尺寸选择，不进行限流。</p>
 */
@RestController
@RequestMapping("/api/images")
public class PublicImageController {

    private static final Logger log = LoggerFactory.getLogger(PublicImageController.class);

    private final ImageService imageService;
    private final ClientIpUtil clientIpUtil;

    public PublicImageController(ImageService imageService, ClientIpUtil clientIpUtil) {
        this.imageService = imageService;
        this.clientIpUtil = clientIpUtil;
    }

    /**
     * 通过哈希值获取图片
     *
     * <p>支持服务端图片压缩和缓存。
     * 使用动态 IP 限流和速度调整防止后端过载。</p>
     *
     * @param hash     图片哈希值（MD5）
     * @param size     图片尺寸规格（可选，默认 lg 大图 1080px 宽）
     *                  - sm: 小图 256px 宽
     *                  - lg: 大图 1080px 宽（默认）
     *                  - o:  原图
     * @param request  HTTP 请求
     * @param response HTTP 响应
     */
    @RateLimit(count = 120, period = 1, prefix = "public_image_get")
    @GetMapping("/{hash}")
    public void getImage(@PathVariable String hash,
                         @RequestParam(required = false, defaultValue = "lg") String size,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        String clientIp = getClientIp(request);
        long startTime = System.currentTimeMillis();

        OSSConfig.ImageSize imageSize = OSSConfig.ImageSize.fromCode(size);
        String sizeCode = imageSize.getCode();
        String sizeDesc = imageSize.getDescription();

        log.debug("[图片请求] hash=[{}], size=[{}][{}], IP=[{}]",
                hash, sizeCode, sizeDesc, clientIp);

        try {
            ImageMeta meta = imageService.getImageMeta(hash);
            if (meta == null) {
                log.warn("[图片不存在] hash=[{}]", hash);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            response.setContentType(meta.contentType());
            response.setStatus(HttpStatus.OK.value());

            log.debug("[开始传输] hash=[{}], size=[{}], 大小=[{} bytes], 类型=[{}]",
                    hash, sizeCode, meta.contentLength(), meta.contentType());
            boolean success = imageService.streamImage(hash, imageSize, response);

            if (!success && !response.isCommitted()) {
                log.error("[传输失败] hash=[{}], 响应状态未提交，将返回503", hash);
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            } else if (!success && response.isCommitted()) {
                log.warn("[传输失败但已提交] hash=[{}], 可能是客户端断开连接", hash);
            } else {
                long duration = System.currentTimeMillis() - startTime;
                log.debug("[传输成功] hash=[{}], size=[{}], 耗时=[{}ms], 大小=[{} bytes]",
                                hash, sizeCode, duration, meta.contentLength());
            }
        } catch (Exception e) {
            log.error("[图片获取异常] hash=[{}]：{}", hash, e.getMessage(), e);
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * 获取客户端真实 IP（仅信任可信代理来源的转发头，防止伪造）
     */
    private String getClientIp(HttpServletRequest request) {
        return clientIpUtil.getClientIp(request);
    }
}
