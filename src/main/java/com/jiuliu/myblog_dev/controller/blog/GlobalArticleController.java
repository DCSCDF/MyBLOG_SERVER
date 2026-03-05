/*
 * [GlobalArticleController.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/6
 */

package com.jiuliu.myblog_dev.controller.blog;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.blog.global.*;
import com.jiuliu.myblog_dev.service.blog.GlobalArticleService;
import com.jiuliu.myblog_dev.utils.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 全局文章管理Controller
 */
@RestController
@RequestMapping("/api/global-article")
public class GlobalArticleController {

    private final GlobalArticleService globalArticleService;

    public GlobalArticleController(GlobalArticleService globalArticleService) {
        this.globalArticleService = globalArticleService;
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
     * 分页获取所有文章列表
     * POST /api/global-article/list
     * 权限：system:article:list
     */
    @SaCheckPermission("system:article:list")
    @PostMapping("/list")
    public Response<PageGlobalArticleResponseDTO> getPageGlobalArticles(@Valid @RequestBody PageGlobalArticleDTO dto) {
        SaResult saResult = globalArticleService.getPageGlobalArticles(dto);
        return handleSaResultGeneral(saResult);
    }

    /**
     * 更新文章状态（隐藏、置顶、推荐）
     * PUT /api/global-article/{id}/status
     * 权限：system:article:edit
     */
    @SaCheckPermission("system:article:edit")
    @PutMapping("/{id}/status")
    public Response<Map<String, Object>> updateArticleStatus(@PathVariable Long id,
                                                             @Valid @RequestBody GlobalArticleStatusUpdateDTO dto) {
        SaResult saResult = globalArticleService.updateArticleStatus(id, dto);
        return handleSaResult(saResult);
    }

    /**
     * 删除文章
     * DELETE /api/global-article/{id}
     * 权限：system:article:delete
     */
    @SaCheckPermission("system:article:delete")
    @DeleteMapping("/{id}")
    public Response<Map<String, Object>> deleteArticle(@PathVariable Long id) {
        SaResult saResult = globalArticleService.deleteArticle(id);
        return handleSaResult(saResult);
    }
}
