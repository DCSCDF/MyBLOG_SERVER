/*
 * [GlobalArticleResponseDTO.java]
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

package com.jiuliu.myblog_dev.dto.blog.global;

import lombok.Data;

/**
 * 全局文章管理响应DTO
 */
@Data
public class GlobalArticleResponseDTO {

    /**
     * 文章ID
     */
    private Long id;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章摘要（如果为空则从HTML内容提取前50字）
     */
    private String summary;

    /**
     * 封面图片URL
     */
    private String coverImage;

    /**
     * 标签（逗号分隔）
     */
    private String tags;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 是否隐藏
     */
    private Boolean isHidden;

    /**
     * 是否置顶
     */
    private Boolean isTop;

    /**
     * 是否推荐
     */
    private Boolean isRecommend;

    /**
     * 作者ID
     */
    private Long authorId;

    /**
     * 作者昵称（用户已注销时为空）
     */
    private String authorNickname;
}
