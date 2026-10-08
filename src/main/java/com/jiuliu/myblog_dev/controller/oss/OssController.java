/*
 * [OssController.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/26
 */

package com.jiuliu.myblog_dev.controller.oss;

import com.jiuliu.myblog_dev.utils.security.SecurityUtils;

import com.jiuliu.myblog_dev.dto.ServiceResult;

import org.springframework.security.access.prepost.PreAuthorize;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.oss.PageUserOssDTO;
import com.jiuliu.myblog_dev.dto.oss.PageUserOssResponseDTO;
import com.jiuliu.myblog_dev.service.oss.OssService;
import com.jiuliu.myblog_dev.service.oss.OssServiceImpl;
import com.jiuliu.myblog_dev.utils.rateLimit.RateLimit;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/oss")
public class OssController {

    private static final Logger log = LoggerFactory.getLogger(OssController.class);

    private final OssService ossService;

    public OssController(OssService ossService) {
        this.ossService = ossService;
    }

    /**
     * 测试 OSS 连接
     * 权限：system:config:edit
     */
    @PreAuthorize("hasAuthority('system:config:edit')")
    @GetMapping("/test")
    public Response<Object> testConnection() {
        ServiceResult serviceResult = ossService.testConnection();
        return handleServiceResult(serviceResult);
    }

    /**
     * 上传图片
     * 权限：oss:create
     *
     * <p>支持格式：jpg, jpeg, png, gif, bmp, webp<br>
     * 上传前会进行格式校验和无损压缩。<br>
     * 响应中返回图片的 MD5 哈希值，前端可用于访问图片。</p>
     */
    @PreAuthorize("hasAuthority('oss:create')")
    @RateLimit(count = 20, period = 1, prefix = "oss_upload")
    @PostMapping("/upload")
    public Response<OssServiceImpl.ImageUploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseUtil.fail("请选择要上传的图片", 400);
        }

        try {
            String fileName = file.getOriginalFilename();
            byte[] bytes = file.getBytes();
            String contentType = file.getContentType();
            Long userId = getCurrentUserId();

            ServiceResult result = ossService.uploadImage(fileName, bytes, contentType, userId);
            return handleServiceResultWithData(result);
        } catch (Exception e) {
            // 内部异常细节只进日志，避免泄露 OSS/SMTP 等内部信息
            log.error("图片上传异常：{}", e.getMessage(), e);
            return ResponseUtil.fail("图片上传失败，请稍后重试", 500);
        }
    }

//    /**
//     * 删除图片（通过对象名称）
//     * 权限：oss:delete
//     *
//     * @param objectName OSS 对象名称（即文件路径，如 images/2026/03/26/xxx.jpg）
//     */
//    @PreAuthorize("hasAuthority('oss:delete')")
//    @DeleteMapping("/delete")
//    public Response<Object> deleteImage(@RequestParam("objectName") String objectName) {
//        Long userId = getCurrentUserId();
//        ServiceResult result = ossService.deleteImage(objectName, userId);
//        return handleServiceResult(result);
//    }

    /**
     * 分页获取当前用户的OSS图片列表
     * 权限：oss:list
     */
    @PreAuthorize("hasAuthority('oss:list')")
    @PostMapping("/list")
    public Response<PageUserOssResponseDTO> getPageUserOssImages(@Valid @RequestBody PageUserOssDTO dto) {
        Long userId = getCurrentUserId();
        ServiceResult serviceResult = ossService.getPageUserOssImages(dto, userId);
        return handleServiceResult(serviceResult);
    }

    /**
     * 删除图片（通过哈希值）
     * 权限：oss:delete
     *
     * @param hash 图片哈希值（MD5）
     */
    @PreAuthorize("hasAuthority('oss:delete')")
    @DeleteMapping("/delete/{hash}")
    public Response<Object> deleteImageByHash(@PathVariable String hash) {
        Long userId = getCurrentUserId();
        ServiceResult result = ossService.deleteImageByHash(hash, userId);
        return handleServiceResult(result);
    }

    /**
     * 获取当前登录用户 ID
     */
    private Long getCurrentUserId() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private <T> Response<T> handleServiceResult(ServiceResult serviceResult) {
        if (serviceResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            T data = (T) serviceResult.getData();
            return ResponseUtil.success(data, 200);
        }
        return ResponseUtil.fail(serviceResult.getMsg(), serviceResult.getCode());
    }

    private Response<OssServiceImpl.ImageUploadResponse> handleServiceResultWithData(ServiceResult serviceResult) {
        if (serviceResult.getCode() == 200) {
            OssServiceImpl.ImageUploadResponse data = (OssServiceImpl.ImageUploadResponse) serviceResult.getData();
            return ResponseUtil.success(data, 200);
        }
        return ResponseUtil.fail(serviceResult.getMsg(), serviceResult.getCode());
    }
}
