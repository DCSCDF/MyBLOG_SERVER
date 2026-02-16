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
import com.jiuliu.myblog_dev.dto.user.permissiongroup.PermissionGroupResponseDTO;
import com.jiuliu.myblog_dev.dto.user.permissiongroup.PermissionGroupUpdateDTO;

public interface PermissionGroupService {

    SaResult getPagePermissionGroups(PagePermissionGroupDTO pageDto);

    SaResult getPermissionGroupById(Long id);

    SaResult updatePermissionGroup(PermissionGroupUpdateDTO dto);

    SaResult deletePermissionGroup(Long id);
}
