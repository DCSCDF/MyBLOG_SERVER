/*
 * [UserController.java]
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

package com.jiuliu.myblog_dev.controller.user;


import org.springframework.security.access.prepost.PreAuthorize;
import com.jiuliu.myblog_dev.common.ServiceResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.user.manage.*;
import com.jiuliu.myblog_dev.dto.user.role.RoleResponseDTO;
import com.jiuliu.myblog_dev.service.user.manage.UserManageService;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
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
    @PreAuthorize("hasAuthority('system:user:list')")
    @PostMapping("/list")
    public Response<PageUserResponseDTO> getPageUsers(@Valid @RequestBody PageUserDTO pageDto) {
        ServiceResult serviceResult = userManageService.getPageUsers(pageDto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 获取用户详情
     * 权限：system:user:list
     */
    @PreAuthorize("hasAuthority('system:user:list')")
    @GetMapping("/{id}")
    public Response<UserAdminResponseDTO> getUserById(@PathVariable Long id) {
        ServiceResult serviceResult = userManageService.getUserById(id);
        return handleServiceResult(serviceResult);
    }

    /**
     * 获取用户角色列表
     * 权限：system:user:assignRole
     */
    @PreAuthorize("hasAuthority('system:user:assignRole')")
    @GetMapping("/{id}/roles")
    public Response<List<RoleResponseDTO>> getUserRoles(@PathVariable Long id) {
        ServiceResult serviceResult = userManageService.getUserRoles(id);
        return handleServiceResult(serviceResult);
    }

    /**
     * 修改用户信息（昵称/头像/角色）
     * 权限：system:user:edit
     */
    @PreAuthorize("hasAuthority('system:user:edit')")
    @PutMapping("/{id}")
    public Response<UserAdminResponseDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        ServiceResult serviceResult = userManageService.updateUser(id, dto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 启用/禁用用户
     * 权限：system:user:edit
     */
    @PreAuthorize("hasAuthority('system:user:edit')")
    @PutMapping("/{id}/status")
    public Response<Object> updateUserStatus(@PathVariable Long id, @Valid @RequestBody UserUpdateStatusDTO dto) {
        ServiceResult serviceResult = userManageService.updateUserStatus(id, dto.getStatus());
        return handleServiceResult(serviceResult);
    }

    /**
     * 删除用户（逻辑删除）
     * 权限：system:user:delete
     */
    @PreAuthorize("hasAuthority('system:user:delete')")
    @DeleteMapping("/{id}")
    public Response<Object> deleteUser(@PathVariable Long id) {
        ServiceResult serviceResult = userManageService.deleteUser(id);
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
