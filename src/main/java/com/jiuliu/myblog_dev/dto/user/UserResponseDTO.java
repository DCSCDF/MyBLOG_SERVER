/*
 * [UserResponseDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/18 11:52
 */

package com.jiuliu.myblog_dev.dto.user;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应DTO
 * 用于对外暴露用户基本信息，隐藏敏感字段如密码
 */
@Data
public class UserResponseDTO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String avatarUrl;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}