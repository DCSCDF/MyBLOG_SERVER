/*
 * [RoleController.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.controller.user.role;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.user.role.PageRoleDTO;
import com.jiuliu.myblog_dev.dto.user.role.PageRoleResponseDTO;
import com.jiuliu.myblog_dev.dto.user.role.RoleCreateDTO;
import com.jiuliu.myblog_dev.dto.user.role.RolePermissionGroupItemDTO;
import com.jiuliu.myblog_dev.dto.user.role.RolePermissionItemDTO;
import com.jiuliu.myblog_dev.dto.user.role.RolePermissionsDetailDTO;
import com.jiuliu.myblog_dev.dto.user.role.RoleResponseDTO;
import com.jiuliu.myblog_dev.dto.user.role.RoleUpdateDTO;
import com.jiuliu.myblog_dev.service.user.role.RoleService;
import com.jiuliu.myblog_dev.utils.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/role")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 分页获取角色列表
     * 权限：system:role:list
     */
    @SaCheckPermission("system:role:list")
    @PostMapping("/list")
    public Response<PageRoleResponseDTO> getPageRoles(@Valid @RequestBody PageRoleDTO pageDto) {
        SaResult saResult = roleService.getPageRoles(pageDto);
        return handleSaResult(saResult, PageRoleResponseDTO.class);
    }

    /**
     * 根据ID获取角色详情
     * 权限：system:role:list
     */
    @SaCheckPermission("system:role:list")
    @GetMapping("/{id}")
    public Response<RoleResponseDTO> getRoleById(@PathVariable Long id) {
        SaResult saResult = roleService.getRoleById(id);
        return handleSaResult(saResult, RoleResponseDTO.class);
    }

    /**
     * 创建角色
     * 权限：system:role:create
     */
    @SaCheckPermission("system:role:create")
    @PostMapping
    public Response<RoleResponseDTO> createRole(@Valid @RequestBody RoleCreateDTO dto) {
        SaResult saResult = roleService.createRole(dto);
        return handleSaResult(saResult, RoleResponseDTO.class);
    }

    /**
     * 修改角色（系统内置角色不可修改）
     * 权限：system:role:edit
     */
    @SaCheckPermission("system:role:edit")
    @PutMapping("/{id}")
    public Response<RoleResponseDTO> updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateDTO dto) {
        dto.setId(id);
        SaResult saResult = roleService.updateRole(dto);
        return handleSaResult(saResult, RoleResponseDTO.class);
    }

    /**
     * 删除角色（系统内置角色不可删除，同时级联删除所有关联）
     * 权限：system:role:delete
     */
    @SaCheckPermission("system:role:delete")
    @DeleteMapping("/{id}")
    public Response<Object> deleteRole(@PathVariable Long id) {
        SaResult saResult = roleService.deleteRole(id);
        return handleSaResult(saResult, Object.class);
    }

    /**
     * 获取角色关联的权限列表和权限组列表
     * 权限：system:role:list
     */
    @SaCheckPermission("system:role:list")
    @GetMapping("/{id}/permissions-detail")
    public Response<RolePermissionsDetailDTO> getRolePermissionsDetail(@PathVariable Long id) {
        SaResult saResult = roleService.getRolePermissionsDetail(id);
        return handleSaResult(saResult, RolePermissionsDetailDTO.class);
    }

    /**
     * 为角色添加权限（仅非系统内置角色可操作）
     * 权限：system:role:addPermission
     */
    @SaCheckPermission("system:role:addPermission")
    @PostMapping("/{id}/permissions")
    public Response<Object> addPermissionToRole(
            @PathVariable Long id, @Valid @RequestBody RolePermissionItemDTO dto) {
        SaResult saResult = roleService.addPermissionToRole(id, dto.getPermissionId());
        return handleSaResult(saResult, Object.class);
    }

    /**
     * 从角色移除权限（仅非系统内置角色可操作）
     * 权限：system:role:removePermission
     */
    @SaCheckPermission("system:role:removePermission")
    @DeleteMapping("/{id}/permissions/{permissionId}")
    public Response<Object> removePermissionFromRole(
            @PathVariable Long id, @PathVariable Long permissionId) {
        SaResult saResult = roleService.removePermissionFromRole(id, permissionId);
        return handleSaResult(saResult, Object.class);
    }

    /**
     * 为角色添加权限组（仅非系统内置角色可操作）
     * 权限：system:role:addPermissionGroup
     */
    @SaCheckPermission("system:role:addPermissionGroup")
    @PostMapping("/{id}/permission-groups")
    public Response<Object> addPermissionGroupToRole(
            @PathVariable Long id, @Valid @RequestBody RolePermissionGroupItemDTO dto) {
        SaResult saResult = roleService.addPermissionGroupToRole(id, dto.getGroupId());
        return handleSaResult(saResult, Object.class);
    }

    /**
     * 从角色移除权限组（仅非系统内置角色可操作）
     * 权限：system:role:removePermissionGroup
     */
    @SaCheckPermission("system:role:removePermissionGroup")
    @DeleteMapping("/{id}/permission-groups/{groupId}")
    public Response<Object> removePermissionGroupFromRole(
            @PathVariable Long id, @PathVariable Long groupId) {
        SaResult saResult = roleService.removePermissionGroupFromRole(id, groupId);
        return handleSaResult(saResult, Object.class);
    }

    private <T> Response<T> handleSaResult(SaResult saResult, Class<T> dataType) {
        if (saResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            T data = (T) saResult.getData();
            return ResponseUtil.success(data, 200);
        }
        return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
    }
}
