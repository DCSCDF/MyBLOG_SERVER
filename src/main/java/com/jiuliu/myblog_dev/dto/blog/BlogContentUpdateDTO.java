/*
 * [BlogContentUpdateDTO.java]
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

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BlogContentUpdateDTO {

    /**
     * 文章标题（必填）
     */
    @Size(max = 200, message = "文章标题不能超过200字符")
    private String title;

    /**
     * 文章摘要（最大200字个）
     */
    @Size(max = 200, message = "文章摘要不能超过200字符")
    private String summary;

    /**
     * MD文章内容
     */
    private String content;

    /**
     * 封面图片URL
     */
    private String coverImage;

    /**
     * 标签（多个标签使用逗号分隔）
     */
    private String tags;

    /**
     * 分类ID
     */
    private Long categoryId;
}
