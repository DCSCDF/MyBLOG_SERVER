/*
 * [UserManageService.java]
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
 * [UserManageService.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.service.user.manage;

import com.jiuliu.myblog_dev.common.ServiceResult;
import com.jiuliu.myblog_dev.dto.user.manage.PageUserDTO;
import com.jiuliu.myblog_dev.dto.user.manage.UserUpdateDTO;

public interface UserManageService {

    ServiceResult getPageUsers(PageUserDTO pageDto);

    ServiceResult getUserById(Long id);

    ServiceResult getUserRoles(Long userId);

    ServiceResult updateUser(Long id, UserUpdateDTO dto);

    ServiceResult updateUserStatus(Long id, Integer status);

    ServiceResult deleteUser(Long id);
}

