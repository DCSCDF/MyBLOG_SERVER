/*
 * [RoleUpdateDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.dto.user.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleUpdateDTO {

    private Long id;  // 由 Controller 从路径参数设置

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50)
    private String name;

    @Size(max = 200)
    private String description;

    private Integer sortOrder;
    private Integer status;  // 0=禁用，1=启用
}
