/*
 * [RoleController.java]
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
 * [RoleController.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.controller.user.role;

import com.jiuliu.myblog_dev.dto.ServiceResult;

import org.springframework.security.access.prepost.PreAuthorize;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.user.role.*;
import com.jiuliu.myblog_dev.service.user.role.RoleService;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
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
    @PreAuthorize("hasAuthority('system:role:list')")
    @PostMapping("/list")
    public Response<PageRoleResponseDTO> getPageRoles(@Valid @RequestBody PageRoleDTO pageDto) {
        ServiceResult serviceResult = roleService.getPageRoles(pageDto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 根据ID获取角色详情
     * 权限：system:role:list
     */
    @PreAuthorize("hasAuthority('system:role:list')")
    @GetMapping("/{id}")
    public Response<RoleResponseDTO> getRoleById(@PathVariable Long id) {
        ServiceResult serviceResult = roleService.getRoleById(id);
        return handleServiceResult(serviceResult);
    }

    /**
     * 创建角色
     * 权限：system:role:create
     */
    @PreAuthorize("hasAuthority('system:role:create')")
    @PostMapping
    public Response<RoleResponseDTO> createRole(@Valid @RequestBody RoleCreateDTO dto) {
        ServiceResult serviceResult = roleService.createRole(dto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 修改角色（系统内置角色不可修改）
     * 权限：system:role:edit
     */
    @PreAuthorize("hasAuthority('system:role:edit')")
    @PutMapping("/{id}")
    public Response<RoleResponseDTO> updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateDTO dto) {
        dto.setId(id);
        ServiceResult serviceResult = roleService.updateRole(dto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 删除角色（系统内置角色不可删除，同时级联删除所有关联）
     * 权限：system:role:delete
     */
    @PreAuthorize("hasAuthority('system:role:delete')")
    @DeleteMapping("/{id}")
    public Response<Object> deleteRole(@PathVariable Long id) {
        ServiceResult serviceResult = roleService.deleteRole(id);
        return handleServiceResult(serviceResult);
    }

    /**
     * 获取角色关联的权限列表和权限组列表
     * 权限：system:role:list
     */
    @PreAuthorize("hasAuthority('system:role:list')")
    @GetMapping("/{id}/permissions-detail")
    public Response<RolePermissionsDetailDTO> getRolePermissionsDetail(@PathVariable Long id) {
        ServiceResult serviceResult = roleService.getRolePermissionsDetail(id);
        return handleServiceResult(serviceResult);
    }

    /**
     * 为角色添加权限（仅非系统内置角色可操作）
     * 权限：system:role:addPermission
     */
    @PreAuthorize("hasAuthority('system:role:addPermission')")
    @PostMapping("/{id}/permissions")
    public Response<Object> addPermissionToRole(
            @PathVariable Long id, @Valid @RequestBody RolePermissionItemDTO dto) {
        ServiceResult serviceResult = roleService.addPermissionToRole(id, dto.getPermissionId());
        return handleServiceResult(serviceResult);
    }

    /**
     * 从角色移除权限（仅非系统内置角色可操作）
     * 权限：system:role:removePermission
     */
    @PreAuthorize("hasAuthority('system:role:removePermission')")
    @DeleteMapping("/{id}/permissions/{permissionId}")
    public Response<Object> removePermissionFromRole(
            @PathVariable Long id, @PathVariable Long permissionId) {
        ServiceResult serviceResult = roleService.removePermissionFromRole(id, permissionId);
        return handleServiceResult(serviceResult);
    }

    /**
     * 为角色添加权限组（仅非系统内置角色可操作）
     * 权限：system:role:addPermissionGroup
     */
    @PreAuthorize("hasAuthority('system:role:addPermissionGroup')")
    @PostMapping("/{id}/permission-groups")
    public Response<Object> addPermissionGroupToRole(
            @PathVariable Long id, @Valid @RequestBody RolePermissionGroupItemDTO dto) {
        ServiceResult serviceResult = roleService.addPermissionGroupToRole(id, dto.getGroupId());
        return handleServiceResult(serviceResult);
    }

    /**
     * 从角色移除权限组（仅非系统内置角色可操作）
     * 权限：system:role:removePermissionGroup
     */
    @PreAuthorize("hasAuthority('system:role:removePermissionGroup')")
    @DeleteMapping("/{id}/permission-groups/{groupId}")
    public Response<Object> removePermissionGroupFromRole(
            @PathVariable Long id, @PathVariable Long groupId) {
        ServiceResult serviceResult = roleService.removePermissionGroupFromRole(id, groupId);
        return handleServiceResult(serviceResult);
    }

    private <T> Response<T> handleServiceResult(ServiceResult serviceResult) {
        if (serviceResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            T data = (T) serviceResult.getData();
            return ResponseUtil.success(data, 200);
        }
        return ResponseUtil.fail(serviceResult.getMsg(), serviceResult.getCode());
    }
}
