/*
 * [CategoryService.java]
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

package com.jiuliu.myblog_dev.service.blog.category;

import com.jiuliu.myblog_dev.common.ServiceResult;
import com.jiuliu.myblog_dev.dto.blog.category.CategoryCreateDTO;
import com.jiuliu.myblog_dev.dto.blog.category.CategoryUpdateDTO;
import com.jiuliu.myblog_dev.dto.blog.category.PageCategoryDTO;

public interface CategoryService {

    ServiceResult getPageCategories(PageCategoryDTO pageDto);

    ServiceResult getAvailableCategories();

    ServiceResult createCategory(CategoryCreateDTO dto);

    ServiceResult updateCategory(CategoryUpdateDTO dto);

    ServiceResult deleteCategory(Long id);
}

