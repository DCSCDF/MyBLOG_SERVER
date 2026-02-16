/*
 * [RoleResponseDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.dto.user.role;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoleResponseDTO {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean superAdmin;
    private Boolean isSystem;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
