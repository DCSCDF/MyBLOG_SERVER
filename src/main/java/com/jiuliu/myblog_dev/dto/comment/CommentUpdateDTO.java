/*
 * [CommentUpdateDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/8
 */

package com.jiuliu.myblog_dev.dto.comment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新评论DTO
 */
@Data
public class CommentUpdateDTO {

    @NotNull(message = "评论ID不能为空")
    private Long id;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 个人网站
     */
    private String website;
}
