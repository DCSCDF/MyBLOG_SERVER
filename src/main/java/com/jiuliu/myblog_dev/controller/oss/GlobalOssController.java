/*
 * [GlobalOssController.java]
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
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.oss.global.PageGlobalOssDTO;
import com.jiuliu.myblog_dev.dto.oss.global.PageGlobalOssResponseDTO;
import com.jiuliu.myblog_dev.service.oss.GlobalOssService;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 全局OSS管理Controller
 */
@RestController
@RequestMapping("/api/global-oss")
public class GlobalOssController {

    private final GlobalOssService globalOssService;

    public GlobalOssController(GlobalOssService globalOssService) {
        this.globalOssService = globalOssService;
    }

    /**
     * 通用方法：处理SaResult结果
     */
    private <T> Response<T> handleSaResultGeneral(SaResult saResult) {
        if (saResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            T data = (T) saResult.getData();
            return ResponseUtil.success(data, 200);
        } else {
            return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
        }
    }

    /**
     * 处理返回Map类型的SaResult结果
     */
    private Response<Map<String, Object>> handleSaResult(SaResult saResult) {
        if (saResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) saResult.getData();
            return ResponseUtil.success(data, 200);
        } else {
            return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
        }
    }

    /**
     * 分页获取所有OSS图片列表
     * POST /api/global-oss/list
     * 权限：system:oss:list
     */
    @SaCheckPermission("system:oss:list")
    @PostMapping("/list")
    public Response<PageGlobalOssResponseDTO> getPageGlobalOssImages(@Valid @RequestBody PageGlobalOssDTO dto) {
        SaResult saResult = globalOssService.getPageGlobalOssImages(dto);
        return handleSaResultGeneral(saResult);
    }

    /**
     * 删除图片（管理员权限，可删除所有用户的图片）
     * DELETE /api/global-oss/{hash}
     * 权限：system:oss:delete
     */
    @SaCheckPermission("system:oss:delete")
    @DeleteMapping("/{hash}")
    public Response<Map<String, Object>> deleteImage(@PathVariable String hash) {
        SaResult saResult = globalOssService.deleteImage(hash);
        return handleSaResult(saResult);
    }
}
