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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer pageSize;

    /**
     * 搜索关键词：匹配文章标题和摘要（上限50字符，防止超长关键词正则风暴）
     */
    @Size(max = 50, message = "搜索关键词不能超过50字符")
    private String keyword;

    /**
     * 分类ID：用于筛选指定分类的文章
     * 如果传入分类ID，则只返回该分类下的文章
     * 同时关键词搜索也只在该分类内搜索
     */
    private Long categoryId;

    /**
     * 用户名：用于筛选特定用户的文章
     * 如果传入username，则只返回该用户发布的文章
     */
    private String username;
}
