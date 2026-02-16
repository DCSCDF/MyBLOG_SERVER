/*
 * [RolePermissionGroupItemDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.dto.user.role;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RolePermissionGroupItemDTO {

    @NotNull(message = "权限组ID不能为空")
    private Long groupId;
}
