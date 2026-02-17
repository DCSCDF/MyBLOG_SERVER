/*
 * [PermissionGroupService.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.service.user.permissiongroup;

import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.user.permissiongroup.PagePermissionGroupDTO;
import com.jiuliu.myblog_dev.dto.user.permissiongroup.PagePermissionGroupResponseDTO;
import com.jiuliu.myblog_dev.dto.user.permissiongroup.PermissionGroupCreateDTO;
import com.jiuliu.myblog_dev.dto.user.permissiongroup.PermissionGroupResponseDTO;
import com.jiuliu.myblog_dev.dto.user.permissiongroup.PermissionGroupUpdateDTO;

public interface PermissionGroupService {

    SaResult getPagePermissionGroups(PagePermissionGroupDTO pageDto);

    SaResult getPermissionGroupById(Long id);

    SaResult createPermissionGroup(PermissionGroupCreateDTO dto);

    SaResult updatePermissionGroup(PermissionGroupUpdateDTO dto);

    SaResult deletePermissionGroup(Long id);

    /**
     * 获取权限组关联的权限列表（仅非系统内置权限组可修改，但均可查看）
     */
    SaResult getPermissionsByGroupId(Long groupId);

    /**
     * 为权限组添加权限（仅非系统内置权限组可操作）
     */
    SaResult addPermissionToGroup(Long groupId, Long permissionId);

    /**
     * 从权限组移除权限（仅非系统内置权限组可操作）
     */
    SaResult removePermissionFromGroup(Long groupId, Long permissionId);
}
