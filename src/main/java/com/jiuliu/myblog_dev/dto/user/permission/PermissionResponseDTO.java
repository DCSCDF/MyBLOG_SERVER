/*
 * [PermissionResponseDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/16 17:31
 */

package com.jiuliu.myblog_dev.dto.user.permission;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限信息响应DTO
 * 用于对外暴露权限基本信息
 */
@Data
public class PermissionResponseDTO {

    private Long id;

    private String code;

    private String name;

    private String description;

    private Integer sortOrder;

    private LocalDateTime createTime;
}