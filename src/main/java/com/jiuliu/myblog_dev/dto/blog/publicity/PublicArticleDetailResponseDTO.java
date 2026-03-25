/*
 * [PublicArticleDetailResponseDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/23
 */

package com.jiuliu.myblog_dev.dto.blog.publicity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公共文章详情响应DTO
 */
@Data
public class PublicArticleDetailResponseDTO {

    /**
     * 文章ID
     */
    private Long id;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章内容（HTML格式全文）
     */
    private String htmlContent;

    /**
     * 标签（逗号分隔）
     */
    private String tags;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 是否置顶
     */
    private Boolean isTop;

    /**
     * 作者昵称
     */
    private String authorNickname;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
