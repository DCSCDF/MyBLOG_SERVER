/*
 * [PermissionGroupUpdateDTO.java]
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
public class PermissionGroupUpdateDTO {

    private Long id;  // 由 Controller 从路径参数设置

    @NotBlank(message = "权限组名称不能为空")
    @Size(max = 50)
    private String name;

    @Size(max = 200)
    private String description;

    private Integer sortOrder;
    private Integer status;  // 0=禁用，1=启用
}
