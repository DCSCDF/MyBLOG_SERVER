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

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.service.oss.OssService;
import com.jiuliu.myblog_dev.service.oss.OssServiceImpl;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/oss")
public class OssController {

    private final OssService ossService;

    public OssController(OssService ossService) {
        this.ossService = ossService;
    }

    /**
     * 测试 OSS 连接
     * 权限：system:config:edit
     */
    @SaCheckPermission("system:config:edit")
    @GetMapping("/test")
    public Response<Object> testConnection() {
        SaResult saResult = ossService.testConnection();
        return handleSaResult(saResult);
    }

    /**
     * 上传图片
     * 权限：oss:create
     *
     * <p>支持格式：jpg, jpeg, png, gif, bmp, webp<br>
     * 上传前会进行格式校验和无损压缩。<br>
     * 响应中返回图片的 MD5 哈希值，前端可用于访问图片。</p>
     */
    @SaCheckPermission("oss:create")
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

            SaResult result = ossService.uploadImage(fileName, bytes, contentType, userId);
            return handleSaResultWithData(result);
        } catch (Exception e) {
            return ResponseUtil.fail("图片上传失败：" + e.getMessage(), 500);
        }
    }

//    /**
//     * 删除图片（通过对象名称）
//     * 权限：oss:delete
//     *
//     * @param objectName OSS 对象名称（即文件路径，如 images/2026/03/26/xxx.jpg）
//     */
//    @SaCheckPermission("oss:delete")
//    @DeleteMapping("/delete")
//    public Response<Object> deleteImage(@RequestParam("objectName") String objectName) {
//        Long userId = getCurrentUserId();
//        SaResult result = ossService.deleteImage(objectName, userId);
//        return handleSaResult(result);
//    }

    /**
     * 删除图片（通过哈希值）
     * 权限：oss:delete
     *
     * @param hash 图片哈希值（MD5）
     */
    @SaCheckPermission("oss:delete")
    @DeleteMapping("/delete/{hash}")
    public Response<Object> deleteImageByHash(@PathVariable String hash) {
        SaResult result = ossService.deleteImageByHash(hash);
        return handleSaResult(result);
    }

    /**
     * 获取当前登录用户 ID
     */
    private Long getCurrentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    private <T> Response<T> handleSaResult(SaResult saResult) {
        if (saResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            T data = (T) saResult.getData();
            return ResponseUtil.success(data, 200);
        }
        return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
    }

    private Response<OssServiceImpl.ImageUploadResponse> handleSaResultWithData(SaResult saResult) {
        if (saResult.getCode() == 200) {
            OssServiceImpl.ImageUploadResponse data = (OssServiceImpl.ImageUploadResponse) saResult.getData();
            return ResponseUtil.success(data, 200);
        }
        return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
    }
}
