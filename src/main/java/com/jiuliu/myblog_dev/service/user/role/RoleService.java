/*
 * [RoleService.java]
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

/*
 * [RoleService.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.service.user.role;

import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.user.role.PageRoleDTO;
import com.jiuliu.myblog_dev.dto.user.role.RoleCreateDTO;
import com.jiuliu.myblog_dev.dto.user.role.RoleUpdateDTO;

public interface RoleService {

    SaResult getPageRoles(PageRoleDTO pageDto);

    SaResult getRoleById(Long id);

    SaResult createRole(RoleCreateDTO dto);

    SaResult updateRole(RoleUpdateDTO dto);

    SaResult deleteRole(Long id);

    /**
     * 获取角色关联的权限列表和权限组列表
     */
    SaResult getRolePermissionsDetail(Long roleId);

    /**
     * 为角色添加权限（仅非系统内置角色可操作）
     */
    SaResult addPermissionToRole(Long roleId, Long permissionId);

    /**
     * 从角色移除权限（仅非系统内置角色可操作）
     */
    SaResult removePermissionFromRole(Long roleId, Long permissionId);

    /**
     * 为角色添加权限组（仅非系统内置角色可操作）
     */
    SaResult addPermissionGroupToRole(Long roleId, Long groupId);

    /**
     * 从角色移除权限组（仅非系统内置角色可操作）
     */
    SaResult removePermissionGroupFromRole(Long roleId, Long groupId);
}
