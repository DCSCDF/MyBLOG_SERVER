/*
 * [UserController.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/18 04:42
 */

package com.jiuliu.myblog_dev.controller.user;


import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.user.manage.PageUserDTO;
import com.jiuliu.myblog_dev.dto.user.manage.PageUserResponseDTO;
import com.jiuliu.myblog_dev.dto.user.manage.UserAdminResponseDTO;
import com.jiuliu.myblog_dev.dto.user.manage.UserUpdateDTO;
import com.jiuliu.myblog_dev.dto.user.manage.UserUpdateStatusDTO;
import com.jiuliu.myblog_dev.dto.user.role.RoleResponseDTO;
import com.jiuliu.myblog_dev.service.user.manage.UserManageService;
import com.jiuliu.myblog_dev.utils.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserManageService userManageService;

    public UserController(UserManageService userManageService) {
        this.userManageService = userManageService;
    }

    /**
     * 分页获取用户列表
     * 权限：system:user:list
     */
    @SaCheckPermission("system:user:list")
    @PostMapping("/list")
    public Response<PageUserResponseDTO> getPageUsers(@Valid @RequestBody PageUserDTO pageDto) {
        SaResult saResult = userManageService.getPageUsers(pageDto);
        return handleSaResult(saResult);
    }

    /**
     * 获取用户详情
     * 权限：system:user:list
     */
    @SaCheckPermission("system:user:list")
    @GetMapping("/{id}")
    public Response<UserAdminResponseDTO> getUserById(@PathVariable Long id) {
        SaResult saResult = userManageService.getUserById(id);
        return handleSaResult(saResult);
    }

    /**
     * 获取用户角色列表
     * 权限：system:user:assignRole
     */
    @SaCheckPermission("system:user:assignRole")
    @GetMapping("/{id}/roles")
    public Response<List<RoleResponseDTO>> getUserRoles(@PathVariable Long id) {
        SaResult saResult = userManageService.getUserRoles(id);
        return handleSaResult(saResult);
    }

    /**
     * 修改用户信息（昵称/头像/角色）
     * 权限：system:user:edit
     */
    @SaCheckPermission("system:user:edit")
    @PutMapping("/{id}")
    public Response<UserAdminResponseDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        SaResult saResult = userManageService.updateUser(id, dto);
        return handleSaResult(saResult);
    }

    /**
     * 启用/禁用用户
     * 权限：system:user:edit
     */
    @SaCheckPermission("system:user:edit")
    @PutMapping("/{id}/status")
    public Response<Object> updateUserStatus(@PathVariable Long id, @Valid @RequestBody UserUpdateStatusDTO dto) {
        SaResult saResult = userManageService.updateUserStatus(id, dto.getStatus());
        return handleSaResult(saResult);
    }

    /**
     * 删除用户（逻辑删除）
     * 权限：system:user:delete
     */
    @SaCheckPermission("system:user:delete")
    @DeleteMapping("/{id}")
    public Response<Object> deleteUser(@PathVariable Long id) {
        SaResult saResult = userManageService.deleteUser(id);
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
