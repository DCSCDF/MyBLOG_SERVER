/*
 * [BlogResponseDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/5
 */

package com.jiuliu.myblog_dev.dto.blog;

import lombok.Data;

@Data
public class BlogResponseDTO {

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
     * 文章摘要
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
     * 评论数
     */
    private Integer commentCount;

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
}
