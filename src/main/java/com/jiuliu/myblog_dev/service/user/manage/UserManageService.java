/*
 * [UserManageService.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.service.user.manage;

import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.user.manage.PageUserDTO;
import com.jiuliu.myblog_dev.dto.user.manage.UserUpdateDTO;

public interface UserManageService {

    SaResult getPageUsers(PageUserDTO pageDto);

    SaResult getUserById(Long id);

    SaResult getUserRoles(Long userId);

    SaResult updateUser(Long id, UserUpdateDTO dto);

    SaResult updateUserStatus(Long id, Integer status);

    SaResult deleteUser(Long id);
}

