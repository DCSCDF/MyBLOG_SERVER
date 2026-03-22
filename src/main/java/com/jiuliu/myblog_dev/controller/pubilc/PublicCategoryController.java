/*
 * [PublicCategoryController.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/23
 */

package com.jiuliu.myblog_dev.controller.pubilc;

import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.blog.publicity.PublicCategoryResponseDTO;
import com.jiuliu.myblog_dev.service.blog.PublicCategoryService;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公共分类接口 - 无需登录即可访问
 */
@RestController
@RequestMapping("/api/public/category")
public class PublicCategoryController {

    private final PublicCategoryService publicCategoryService;

    public PublicCategoryController(PublicCategoryService publicCategoryService) {
        this.publicCategoryService = publicCategoryService;
    }

    /**
     * 通用方法：处理SaResult结果
     */
    private <T> Response<T> handleSaResult(SaResult saResult) {
        if (saResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            T data = (T) saResult.getData();
            return ResponseUtil.success(data, 200);
        }
        return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
    }

    /**
     * 获取可显示的分类列表
     * GET /api/public/category/list
     * 无需登录，所有用户均可访问
     * 返回所有未隐藏的分类，包含ID、名称、描述、排序
     */
    @GetMapping("/list")
    public Response<List<PublicCategoryResponseDTO>> getVisibleCategories() {
        SaResult saResult = publicCategoryService.getVisibleCategories();
        return handleSaResult(saResult);
    }

    /**
     * 根据ID获取分类详情
     * GET /api/public/category/{id}
     * 无需登录，所有用户均可访问
     * 仅返回未隐藏的分类
     */
    @GetMapping("/{id}")
    public Response<PublicCategoryResponseDTO> getCategoryById(@PathVariable("id") Long id) {
        SaResult saResult = publicCategoryService.getCategoryById(id);
        return handleSaResult(saResult);
    }
}
