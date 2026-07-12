/*
 * [PublicAdminResponseDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 */

package com.jiuliu.myblog_dev.dto.user;

import lombok.Data;

/**
 * 公共管理员信息响应DTO
 * 用于对外暴露超级管理员的基本公开信息，隐藏敏感字段如密码、ID等
 */
@Data
public class PublicAdminResponseDTO {

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 用户简介
     */
    private String bio;
}