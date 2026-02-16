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
     * 删除角色（系统内置角色不可删除）
     * 权限：system:role:delete
     */
    @SaCheckPermission("system:role:delete")
    @DeleteMapping("/{id}")
    public Response<Object> deleteRole(@PathVariable Long id) {
        SaResult saResult = roleService.deleteRole(id);
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
