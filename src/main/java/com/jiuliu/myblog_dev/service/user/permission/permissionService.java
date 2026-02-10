/*
 * [permissionService.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/1/24 23:24
 */

package com.jiuliu.myblog_dev.service.user.permission;

import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.user.permission.PagePermissionDTO;


public interface permissionService {


//    SaResult getAllPermissions();

    /**
     * 分页获取权限列表
     *
     * @param pageDto 分页参数DTO
     * @return 分页结果
     */
    SaResult getPagePermissions(PagePermissionDTO pageDto);


}
