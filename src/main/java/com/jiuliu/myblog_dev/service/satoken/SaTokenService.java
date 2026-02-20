/*
 * [SaTokenService.java]
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

package com.jiuliu.myblog_dev.service.satoken;

import cn.dev33.satoken.stp.StpInterface;
import com.jiuliu.myblog_dev.entity.user.permission.SysPermission;
import com.jiuliu.myblog_dev.entity.user.role.SysRole;
import com.jiuliu.myblog_dev.mapper.user.permission.SysPermissionMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRoleMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SaTokenService implements StpInterface {

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;

    public SaTokenService(SysRoleMapper sysRoleMapper, SysPermissionMapper sysPermissionMapper) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {

        Long userId = convertToLong(loginId);

        List<SysRole> roleList = sysRoleMapper.selectRolesByUserId(userId);
        return roleList.stream().map(SysRole::getCode).collect(Collectors.toList());
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = convertToLong(loginId);
        List<SysRole> roleList = sysRoleMapper.selectRolesByUserId(userId);

        return roleList.stream()
                .flatMap(role ->
                        sysPermissionMapper.selectPermissionsByRoleId(role.getId()).stream()
                )
                .map(SysPermission::getCode)
                .collect(Collectors.toList());
    }

    private Long convertToLong(Object loginId) {
        if (loginId instanceof Long) {
            return (Long) loginId;
        } else if (loginId instanceof String) {
            return Long.parseLong((String) loginId);
        }
        throw new IllegalArgumentException("Unsupported loginId type: " + loginId.getClass().getName());
    }
}