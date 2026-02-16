/*
 * [PermissionGroupResponseDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.dto.user.permissiongroup;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PermissionGroupResponseDTO {

    private Long id;
    private String name;
    private String description;
    private Integer sortOrder;
    private Integer status;
    private Boolean isSystem;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
