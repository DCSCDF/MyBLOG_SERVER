/*
 * [PermissionGroupController.java]
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
 * [PermissionGroupController.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.controller.user.permissionGroup;

import org.springframework.security.access.prepost.PreAuthorize;
import com.jiuliu.myblog_dev.common.ServiceResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.user.permissiongroup.*;
import com.jiuliu.myblog_dev.service.user.permissiongroup.PermissionGroupService;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/permission-group")
public class PermissionGroupController {

    private final PermissionGroupService permissionGroupService;

    public PermissionGroupController(PermissionGroupService permissionGroupService) {
        this.permissionGroupService = permissionGroupService;
    }

    /**
     * 分页获取权限组列表
     * 权限：system:permission_group:list
     */
    @PreAuthorize("hasAuthority('system:permission:permission_group:list')")
    @PostMapping("/list")
    public Response<PagePermissionGroupResponseDTO> getPagePermissionGroups(
            @Valid @RequestBody PagePermissionGroupDTO pageDto) {
        ServiceResult serviceResult = permissionGroupService.getPagePermissionGroups(pageDto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 根据ID获取权限组详情
     * 权限：system:permission_group:list
     */
    @PreAuthorize("hasAuthority('system:permission:permission_group:list')")
    @GetMapping("/{id}")
    public Response<PermissionGroupResponseDTO> getPermissionGroupById(@PathVariable Long id) {
        ServiceResult serviceResult = permissionGroupService.getPermissionGroupById(id);
        return handleServiceResult(serviceResult);
    }

    /**
     * 创建权限组
     * 权限：system:permission_group:create
     */
    @PreAuthorize("hasAuthority('system:permission:permission_group:create')")
    @PostMapping
    public Response<PermissionGroupResponseDTO> createPermissionGroup(@Valid @RequestBody PermissionGroupCreateDTO dto) {
        ServiceResult serviceResult = permissionGroupService.createPermissionGroup(dto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 修改权限组（系统内置权限组不可修改）
     * 权限：system:permission_group:edit
     */
    @PreAuthorize("hasAuthority('system:permission:permission_group:edit')")
    @PutMapping("/{id}")
    public Response<PermissionGroupResponseDTO> updatePermissionGroup(
            @PathVariable Long id, @Valid @RequestBody PermissionGroupUpdateDTO dto) {
        dto.setId(id);
        ServiceResult serviceResult = permissionGroupService.updatePermissionGroup(dto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 删除权限组（系统内置权限组不可删除；若角色引用则不可删除，需先从相关角色中移除该权限组）
     * 权限：system:permission_group:delete
     */
    @PreAuthorize("hasAuthority('system:permission:permission_group:delete')")
    @DeleteMapping("/{id}")
    public Response<Object> deletePermissionGroup(@PathVariable Long id) {
        ServiceResult serviceResult = permissionGroupService.deletePermissionGroup(id);
        return handleServiceResult(serviceResult);
    }

    /**
     * 获取权限组关联的权限列表
     * 权限：system:permission_group:list
     */
    @PreAuthorize("hasAuthority('system:permission:permission_group:list')")
    @GetMapping("/{id}/permissions")
    public Response<?> getPermissionsByGroupId(@PathVariable Long id) {
        ServiceResult serviceResult = permissionGroupService.getPermissionsByGroupId(id);
        return handleServiceResult(serviceResult);
    }

    /**
     * 为权限组添加权限（仅非系统内置权限组可操作）
     * 权限：system:permission_group:addPermission
     */
    @PreAuthorize("hasAuthority('system:permission:permission_group:addPermission')")
    @PostMapping("/{id}/permissions")
    public Response<Object> addPermissionToGroup(
            @PathVariable Long id, @Valid @RequestBody PermissionGroupItemDTO dto) {
        ServiceResult serviceResult = permissionGroupService.addPermissionToGroup(id, dto.getPermissionId());
        return handleServiceResult(serviceResult);
    }

    /**
     * 从权限组移除权限（仅非系统内置权限组可操作）
     * 权限：system:permission_group:removePermission
     */
    @PreAuthorize("hasAuthority('system:permission:permission_group:removePermission')")
    @DeleteMapping("/{id}/permissions/{permissionId}")
    public Response<Object> removePermissionFromGroup(
            @PathVariable Long id, @PathVariable Long permissionId) {
        ServiceResult serviceResult = permissionGroupService.removePermissionFromGroup(id, permissionId);
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
