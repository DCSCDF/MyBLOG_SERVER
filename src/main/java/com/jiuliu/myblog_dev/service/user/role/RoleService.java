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
import com.jiuliu.myblog_dev.dto.user.role.PageRoleResponseDTO;
import com.jiuliu.myblog_dev.dto.user.role.RoleResponseDTO;
import com.jiuliu.myblog_dev.dto.user.role.RoleUpdateDTO;

public interface RoleService {

    SaResult getPageRoles(PageRoleDTO pageDto);

    SaResult getRoleById(Long id);

    SaResult updateRole(RoleUpdateDTO dto);

    SaResult deleteRole(Long id);
}
