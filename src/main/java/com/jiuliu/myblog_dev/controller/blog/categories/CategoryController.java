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

import org.springframework.security.access.prepost.PreAuthorize;
import com.jiuliu.myblog_dev.common.ServiceResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.blog.category.*;
import com.jiuliu.myblog_dev.service.blog.category.CategoryService;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @PreAuthorize("hasAuthority('category:list')")
    @PostMapping("/list")
    public Response<PageCategoryResponseDTO> getPageCategories(@Valid @RequestBody PageCategoryDTO pageDto) {
        ServiceResult serviceResult = categoryService.getPageCategories(pageDto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 获取可用分类列表（未隐藏）
     * 用于文章创建/编辑时的分类选择
     * 权限：category:list
     */
    @PreAuthorize("hasAuthority('category:list')")
    @GetMapping("/available")
    public Response<List<CategoryResponseDTO>> getAvailableCategories() {
        ServiceResult serviceResult = categoryService.getAvailableCategories();
        return handleServiceResult(serviceResult);
    }

    /**
     * 创建分类
     * 权限：category:create
     */
    @PreAuthorize("hasAuthority('category:create')")
    @PostMapping
    public Response<CategoryResponseDTO> createCategory(@Valid @RequestBody CategoryCreateDTO dto) {
        ServiceResult serviceResult = categoryService.createCategory(dto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 更新分类（名称、描述、排序顺序、是否隐藏）
     * 权限：category:edit
     */
    @PreAuthorize("hasAuthority('category:edit')")
    @PutMapping("/{id}")
    public Response<CategoryResponseDTO> updateCategory(@PathVariable Long id,
                                                        @Valid @RequestBody CategoryUpdateDTO dto) {
        dto.setId(id);
        ServiceResult serviceResult = categoryService.updateCategory(dto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 删除分类
     * 权限：category:delete
     */
    @PreAuthorize("hasAuthority('category:delete')")
    @DeleteMapping("/{id}")
    public Response<Object> deleteCategory(@PathVariable Long id) {
        ServiceResult serviceResult = categoryService.deleteCategory(id);
        return handleServiceResult(serviceResult);
    }

    private <T> Response<T> handleServiceResult(ServiceResult serviceResult) {
        if (serviceResult.getCode() == 200) {

            @SuppressWarnings("unchecked")
            T data = (T) serviceResult.getData();
            return ResponseUtil.success(data, 200);

        }
        return ResponseUtil.fail(serviceResult.getMsg(), serviceResult.getCode());
    }
}

