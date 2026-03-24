/*
 * [PublicCommentCreateDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/24
 */

package com.jiuliu.myblog_dev.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公共评论提交DTO
 */
@Data
public class PublicCommentCreateDTO {

    /**
     * 文章ID
     */
    @NotNull(message = "文章ID不能为空")
    private Long blogId;

    /**
     * 父评论ID，0表示顶级评论
     */
    @NotNull(message = "父评论ID不能为空")
    private Long parentId;

    /**
     * 评论者名称（已登录用户不填）
     */
    @Size(max = 50, message = "评论者名称不能超过50个字符")
    private String username;

    /**
     * 邮箱（已登录用户不填）
     */
    @Size(max = 100, message = "邮箱不能超过100个字符")
    private String email;

    /**
     * 头像URL（已登录用户不填）
     */
    @Size(max = 500, message = "头像URL不能超过500个字符")
    private String avatarUrl;

    /**
     * 个人网站（已登录用户不填）
     */
    @Size(max = 200, message = "个人网站不能超过200个字符")
    private String website;

    /**
     * 评论内容
     */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 2000, message = "评论内容不能超过2000个字符")
    private String content;
}
