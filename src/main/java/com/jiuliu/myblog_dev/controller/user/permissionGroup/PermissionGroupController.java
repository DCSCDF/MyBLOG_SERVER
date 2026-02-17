/*
 * [PermissionGroupController.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.controller.user.permissionGroup;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.user.permissiongroup.PagePermissionGroupDTO;
import com.jiuliu.myblog_dev.dto.user.permissiongroup.PagePermissionGroupResponseDTO;
import com.jiuliu.myblog_dev.dto.user.permissiongroup.PermissionGroupCreateDTO;
import com.jiuliu.myblog_dev.dto.user.permissiongroup.PermissionGroupItemDTO;
import com.jiuliu.myblog_dev.dto.user.permissiongroup.PermissionGroupResponseDTO;
import com.jiuliu.myblog_dev.dto.user.permissiongroup.PermissionGroupUpdateDTO;
import com.jiuliu.myblog_dev.service.user.permissiongroup.PermissionGroupService;
import com.jiuliu.myblog_dev.utils.ResponseUtil;
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
    @SaCheckPermission("system:permission_group:list")
    @PostMapping("/list")
    public Response<PagePermissionGroupResponseDTO> getPagePermissionGroups(
            @Valid @RequestBody PagePermissionGroupDTO pageDto) {
        SaResult saResult = permissionGroupService.getPagePermissionGroups(pageDto);
        return handleSaResult(saResult);
    }

    /**
     * 根据ID获取权限组详情
     * 权限：system:permission_group:list
     */
    @SaCheckPermission("system:permission_group:list")
    @GetMapping("/{id}")
    public Response<PermissionGroupResponseDTO> getPermissionGroupById(@PathVariable Long id) {
        SaResult saResult = permissionGroupService.getPermissionGroupById(id);
        return handleSaResult(saResult);
    }

    /**
     * 创建权限组
     * 权限：system:permission_group:create
     */
    @SaCheckPermission("system:permission_group:create")
    @PostMapping
    public Response<PermissionGroupResponseDTO> createPermissionGroup(@Valid @RequestBody PermissionGroupCreateDTO dto) {
        SaResult saResult = permissionGroupService.createPermissionGroup(dto);
        return handleSaResult(saResult);
    }

    /**
     * 修改权限组（系统内置权限组不可修改）
     * 权限：system:permission_group:edit
     */
    @SaCheckPermission("system:permission_group:edit")
    @PutMapping("/{id}")
    public Response<PermissionGroupResponseDTO> updatePermissionGroup(
            @PathVariable Long id, @Valid @RequestBody PermissionGroupUpdateDTO dto) {
        dto.setId(id);
        SaResult saResult = permissionGroupService.updatePermissionGroup(dto);
        return handleSaResult(saResult);
    }

    /**
     * 删除权限组（系统内置权限组不可删除，同时级联删除所有关联）
     * 权限：system:permission_group:delete
     */
    @SaCheckPermission("system:permission_group:delete")
    @DeleteMapping("/{id}")
    public Response<Object> deletePermissionGroup(@PathVariable Long id) {
        SaResult saResult = permissionGroupService.deletePermissionGroup(id);
        return handleSaResult(saResult);
    }

    /**
     * 获取权限组关联的权限列表
     * 权限：system:permission_group:list
     */
    @SaCheckPermission("system:permission_group:list")
    @GetMapping("/{id}/permissions")
    public Response<?> getPermissionsByGroupId(@PathVariable Long id) {
        SaResult saResult = permissionGroupService.getPermissionsByGroupId(id);
        return handleSaResult(saResult);
    }

    /**
     * 为权限组添加权限（仅非系统内置权限组可操作）
     * 权限：system:permission_group:addPermission
     */
    @SaCheckPermission("system:permission_group:addPermission")
    @PostMapping("/{id}/permissions")
    public Response<Object> addPermissionToGroup(
            @PathVariable Long id, @Valid @RequestBody PermissionGroupItemDTO dto) {
        SaResult saResult = permissionGroupService.addPermissionToGroup(id, dto.getPermissionId());
        return handleSaResult(saResult);
    }

    /**
     * 从权限组移除权限（仅非系统内置权限组可操作）
     * 权限：system:permission_group:removePermission
     */
    @SaCheckPermission("system:permission_group:removePermission")
    @DeleteMapping("/{id}/permissions/{permissionId}")
    public Response<Object> removePermissionFromGroup(
            @PathVariable Long id, @PathVariable Long permissionId) {
        SaResult saResult = permissionGroupService.removePermissionFromGroup(id, permissionId);
        return handleSaResult(saResult);
    }

    private <T> Response<T> handleSaResult(SaResult saResult) {
        if (saResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            T data = (T) saResult.getData();
            return ResponseUtil.success(data, 200);
        }
        return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
    }
}
