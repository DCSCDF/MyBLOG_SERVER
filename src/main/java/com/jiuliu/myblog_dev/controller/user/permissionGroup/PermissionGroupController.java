/*
 * [PermissionGroupController.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.controller.user.permissiongroup;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.user.permissiongroup.*;
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
        return handleSaResult(saResult, PagePermissionGroupResponseDTO.class);
    }

    /**
     * 根据ID获取权限组详情
     * 权限：system:permission_group:list
     */
    @SaCheckPermission("system:permission_group:list")
    @GetMapping("/{id}")
    public Response<PermissionGroupResponseDTO> getPermissionGroupById(@PathVariable Long id) {
        SaResult saResult = permissionGroupService.getPermissionGroupById(id);
        return handleSaResult(saResult, PermissionGroupResponseDTO.class);
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
        return handleSaResult(saResult, PermissionGroupResponseDTO.class);
    }

    /**
     * 删除权限组（系统内置权限组不可删除）
     * 权限：system:permission_group:delete
     */
    @SaCheckPermission("system:permission_group:delete")
    @DeleteMapping("/{id}")
    public Response<Object> deletePermissionGroup(@PathVariable Long id) {
        SaResult saResult = permissionGroupService.deletePermissionGroup(id);
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
