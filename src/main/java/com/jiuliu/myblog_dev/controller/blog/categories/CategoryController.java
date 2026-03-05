/*
 * [CategoryController.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/2
 */

package com.jiuliu.myblog_dev.controller.blog.categories;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.blog.category.*;
import com.jiuliu.myblog_dev.service.blog.category.CategoryService;
import com.jiuliu.myblog_dev.utils.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 分页获取分类列表
     * 权限：category:list
     */
    @SaCheckPermission("category:list")
    @PostMapping("/list")
    public Response<PageCategoryResponseDTO> getPageCategories(@Valid @RequestBody PageCategoryDTO pageDto) {
        SaResult saResult = categoryService.getPageCategories(pageDto);
        return handleSaResult(saResult);
    }

    /**
     * 创建分类
     * 权限：category:create
     */
    @SaCheckPermission("category:create")
    @PostMapping
    public Response<CategoryResponseDTO> createCategory(@Valid @RequestBody CategoryCreateDTO dto) {
        SaResult saResult = categoryService.createCategory(dto);
        return handleSaResult(saResult);
    }

    /**
     * 更新分类（名称、描述、排序顺序、是否隐藏）
     * 权限：category:edit
     */
    @SaCheckPermission("category:edit")
    @PutMapping("/{id}")
    public Response<CategoryResponseDTO> updateCategory(@PathVariable Long id,
                                                        @Valid @RequestBody CategoryUpdateDTO dto) {
        dto.setId(id);
        SaResult saResult = categoryService.updateCategory(dto);
        return handleSaResult(saResult);
    }

    /**
     * 删除分类
     * 权限：category:delete
     */
    @SaCheckPermission("category:delete")
    @DeleteMapping("/{id}")
    public Response<Object> deleteCategory(@PathVariable Long id) {
        SaResult saResult = categoryService.deleteCategory(id);
        return handleSaResult(saResult);
    }

    private <T> Response<T> handleSaResult(SaResult saResult) {
        if (saResult.getCode() == 200) {

            @SuppressWarnings("unchecked")
            T data = (T) saResult.getData();
            return ResponseUtil.success(data, 200);

        }
        return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
    }
}

