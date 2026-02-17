/*
 * [PermissionGroupCreateDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.dto.user.permissiongroup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PermissionGroupCreateDTO {

    @NotBlank(message = "权限组名称不能为空")
    @Size(max = 50, message = "权限组名称最大50字符")
    private String name;

    @Size(max = 200, message = "权限组描述最大200字符")
    private String description;

    private Integer sortOrder = 0;
    private Integer status = 1;  // 0=禁用，1=启用
}
