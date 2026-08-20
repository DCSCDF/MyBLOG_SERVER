/*
 * [BlogCreateDTO.java]
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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BlogCreateDTO {

    /**
     * 文章标题（必填）
     */
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 30, message = "文章标题不能超过30字符")
    private String title;

    /**
     * 分类ID（可以为空）
     */
    private Long categoryId;

    /**
     * 文章摘要（可以为空，最大200个字）
     */
    @Size(max = 200, message = "文章摘要不能超过200字符")
    private String summary;

    /**
     * MD的文章内容（上限 20 万字符，防止超大内容存储/处理放大）
     */
    @Size(max = 200000, message = "文章内容不能超过200000字符")
    private String content;

    /**
     * 封面图片URL（可以为空）
     */
    private String coverImage;

    /**
     * 标签（多个标签使用逗号分隔，如：前端,后端）
     */
    private String tags;
}
