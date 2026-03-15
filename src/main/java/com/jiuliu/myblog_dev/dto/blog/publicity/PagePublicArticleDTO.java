/*
 * [PagePublicArticleDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/14
 */

package com.jiuliu.myblog_dev.dto.blog.publicity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 公共文章列表分页查询DTO
 */
@Data
public class PagePublicArticleDTO {

    @NotNull(message = "当前页码不能为空")
    @Min(value = 1, message = "当前页码必须大于0")
    private Integer currentPage;

    @NotNull(message = "每页数量不能为空")
    @Min(value = 1, message = "每页数量必须大于0")
    private Integer pageSize;

    /**
     * 搜索关键词：匹配文章标题和摘要
     */
    private String keyword;

    /**
     * 分类名称筛选（支持模糊搜索）
     */
    private String categoryName;

    /**
     * 标签名称筛选（支持模糊搜索）
     */
    private String tagName;
}
