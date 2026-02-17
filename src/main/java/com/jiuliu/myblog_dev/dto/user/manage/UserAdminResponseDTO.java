/*
 * [UserAdminResponseDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.dto.user.manage;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户管理响应 DTO（不包含密码等敏感信息）
 */
@Data
public class UserAdminResponseDTO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatarUrl;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

