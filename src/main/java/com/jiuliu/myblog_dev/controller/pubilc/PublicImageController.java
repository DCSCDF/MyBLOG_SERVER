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
 * UpdateTime: 2026/4/1
 */

package com.jiuliu.myblog_dev.controller.pubilc;

import com.jiuliu.myblog_dev.service.oss.ImageService;
import com.jiuliu.myblog_dev.service.oss.ImageService.ImageMeta;
import com.jiuliu.myblog_dev.utils.rateLimit.DynamicRateLimitService;
import com.jiuliu.myblog_dev.utils.rateLimit.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开图片获取接口
 *
 * <p>通过哈希值获取 OSS 中的图片，直接流式传输到客户端。
 * 支持 IP 限流和动态速度调整。</p>
 */
@RestController
@RequestMapping("/api/images")
public class PublicImageController {

    private static final Logger log = LoggerFactory.getLogger(PublicImageController.class);

    private final ImageService imageService;
    private final DynamicRateLimitService dynamicRateLimitService;

    public PublicImageController(ImageService imageService,
                                 DynamicRateLimitService dynamicRateLimitService) {
        this.imageService = imageService;
        this.dynamicRateLimitService = dynamicRateLimitService;
    }

    /**
     * 通过哈希值获取图片
     *
     * <p>直接流式传输 OSS 图片，不缓存。
     * 使用动态 IP 限流和速度调整防止后端过载。</p>
     *
     * @param hash     图片哈希值（MD5）
     * @param request  HTTP 请求
     * @param response HTTP 响应
     */
    @GetMapping("/{hash}")
    @RateLimit(count = 30, period = 1, prefix = "image")
    public void getImage(@PathVariable String hash,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        String clientIp = getClientIp(request);
        long startTime = System.currentTimeMillis();

        log.info("[图片请求] hash=[{}], IP=[{}], 活跃请求=[{}], 推荐速度=[{} KB/s]",
                hash, clientIp,
                dynamicRateLimitService.getActiveRequestCount(),
                dynamicRateLimitService.getRecommendedTransferSpeed());

        if (!dynamicRateLimitService.tryAcquire(clientIp, "image")) {
            log.warn("[限流拒绝] hash=[{}], IP=[{}]", hash, clientIp);
            try {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":429,\"msg\":\"请求过于频繁，请稍后再试\"}");
                response.getWriter().flush();
            } catch (Exception e) {
                log.warn("写入限流响应失败：{}", e.getMessage());
            }
            return;
        }

        try {
            ImageMeta meta = imageService.getImageMeta(hash);
            if (meta == null) {
                log.warn("[图片不存在] hash=[{}]", hash);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            response.setContentType(meta.contentType());
            response.setStatus(HttpStatus.OK.value());

            log.debug("[开始传输] hash=[{}], 大小=[{} bytes], 类型=[{}]", hash, meta.contentLength(), meta.contentType());
            boolean success = imageService.streamImage(hash, response);

            if (!success && !response.isCommitted()) {
                log.error("[传输失败] hash=[{}], 响应状态未提交，将返回503", hash);
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            } else if (!success && response.isCommitted()) {
                log.warn("[传输失败但已提交] hash=[{}], 可能是客户端断开连接", hash);
            } else {
                long duration = System.currentTimeMillis() - startTime;
                log.info("[传输成功] hash=[{}], 耗时=[{}ms], 大小=[{} bytes]", hash, duration, meta.contentLength());
            }
        } catch (Exception e) {
            log.error("[图片获取异常] hash=[{}]：{}", hash, e.getMessage(), e);
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } finally {
            dynamicRateLimitService.release();
        }
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
