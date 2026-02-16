/*
 * [RolePermissionsDetailDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.dto.user.role;

import com.jiuliu.myblog_dev.dto.user.permission.PermissionResponseDTO;
import com.jiuliu.myblog_dev.dto.user.permissiongroup.PermissionGroupResponseDTO;
import lombok.Data;

import java.util.List;

@Data
public class RolePermissionsDetailDTO {

    private RoleResponseDTO role;
    private List<PermissionResponseDTO> permissions;
    private List<PermissionGroupResponseDTO> permissionGroups;
}
