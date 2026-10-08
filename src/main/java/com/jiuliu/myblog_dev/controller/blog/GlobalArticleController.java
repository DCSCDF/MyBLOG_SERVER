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

import com.jiuliu.myblog_dev.dto.ServiceResult;

import org.springframework.security.access.prepost.PreAuthorize;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.blog.global.GlobalArticleStatusUpdateDTO;
import com.jiuliu.myblog_dev.dto.blog.global.PageGlobalArticleDTO;
import com.jiuliu.myblog_dev.dto.blog.global.PageGlobalArticleResponseDTO;
import com.jiuliu.myblog_dev.service.blog.GlobalArticleService;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
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
     * 处理返回Map类型的ServiceResult结果
     */
    private Response<Map<String, Object>> handleServiceResult(ServiceResult serviceResult) {
        if (serviceResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) serviceResult.getData();
            return ResponseUtil.success(data, 200);
        } else {
            return ResponseUtil.fail(serviceResult.getMsg(), serviceResult.getCode());
        }
    }

    /**
     * 通用方法：处理ServiceResult结果
     */
    private <T> Response<T> handleServiceResultGeneral(ServiceResult serviceResult) {
        if (serviceResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            T data = (T) serviceResult.getData();
            return ResponseUtil.success(data, 200);
        } else {
            return ResponseUtil.fail(serviceResult.getMsg(), serviceResult.getCode());
        }
    }

    /**
     * 分页获取所有文章列表
     * POST /api/global-article/list
     * 权限：system:article:list
     */
    @PreAuthorize("hasAuthority('system:article:list')")
    @PostMapping("/list")
    public Response<PageGlobalArticleResponseDTO> getPageGlobalArticles(@Valid @RequestBody PageGlobalArticleDTO dto) {
        ServiceResult serviceResult = globalArticleService.getPageGlobalArticles(dto);
        return handleServiceResultGeneral(serviceResult);
    }

    /**
     * 更新文章状态（隐藏、置顶、推荐）
     * PUT /api/global-article/{id}/status
     * 权限：system:article:edit
     */
    @PreAuthorize("hasAuthority('system:article:edit')")
    @PutMapping("/{id}/status")
    public Response<Map<String, Object>> updateArticleStatus(@PathVariable Long id,
                                                             @Valid @RequestBody GlobalArticleStatusUpdateDTO dto) {
        ServiceResult serviceResult = globalArticleService.updateArticleStatus(id, dto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 删除文章
     * DELETE /api/global-article/{id}
     * 权限：system:article:delete
     */
    @PreAuthorize("hasAuthority('system:article:delete')")
    @DeleteMapping("/{id}")
    public Response<Map<String, Object>> deleteArticle(@PathVariable Long id) {
        ServiceResult serviceResult = globalArticleService.deleteArticle(id);
        return handleServiceResult(serviceResult);
    }
}
