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

package com.jiuliu.myblog_dev.service.authentication;

import cn.dev33.satoken.stp.StpInterface;
import com.jiuliu.myblog_dev.entity.user.permission.SysPermission;
import com.jiuliu.myblog_dev.entity.user.permissiongroup.SysPermissionGroup;
import com.jiuliu.myblog_dev.entity.user.role.SysRole;
import com.jiuliu.myblog_dev.mapper.user.permission.SysPermissionMapper;
import com.jiuliu.myblog_dev.mapper.user.permissionGroup.SysPermissionGroupMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRoleMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRolePermissionMapper;
import com.jiuliu.myblog_dev.utils.security.PermissionOverlapHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthenticationService implements StpInterface {

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysPermissionGroupMapper sysPermissionGroupMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;

    public AuthenticationService(SysRoleMapper sysRoleMapper,
                                 SysPermissionMapper sysPermissionMapper,
                                 SysPermissionGroupMapper sysPermissionGroupMapper,
                                 SysRolePermissionMapper sysRolePermissionMapper) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysPermissionGroupMapper = sysPermissionGroupMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
    }

    //缓存 实现前要确保变更角色的权限后要刷新缓存
    //@Cacheable(value = "userRoles", key = "#loginId"
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {

        Long userId = convertToLong(loginId);

        List<SysRole> roleList = sysRoleMapper.selectRolesByUserId(userId);
        return roleList.stream().map(SysRole::getCode).collect(Collectors.toList());
    }

    //@Cacheable(value = "userPermissions", key = "#loginId")
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = convertToLong(loginId);
        List<SysRole> roleList = sysRoleMapper.selectRolesByUserId(userId);

        // 系统中所有已注册的权限编码，用于展开父权限 -> 子权限
        List<SysPermission> allPermissions = sysPermissionMapper.selectList(null);
        List<String> allPermissionCodes = allPermissions.stream()
                .map(SysPermission::getCode)
                .filter(StringUtils::hasText)
                .toList();

        // 使用有序去重集合，保证返回列表无重复且顺序稳定
        Set<String> resultCodes = new LinkedHashSet<>();

        for (SysRole role : roleList) {
            // 超管角色：直接返回全部权限编码，保证始终有兜底全权限
            if (Boolean.TRUE.equals(role.getSuperAdmin())) {
                resultCodes.addAll(allPermissionCodes);
                break;
            }

            // 权限来源一：通过权限组获取权限（动态计算）
            List<SysPermissionGroup> groups = sysPermissionGroupMapper.selectGroupsByRoleId(role.getId());
            for (SysPermissionGroup group : groups) {
                List<SysPermission> rolePermissions = sysPermissionMapper.selectPermissionsByGroupId(group.getId());
                for (SysPermission permission : rolePermissions) {
                    String code = permission.getCode();
                    if (!StringUtils.hasText(code)) {
                        continue;
                    }
                    // 先加入自身权限
                    if (resultCodes.add(code)) {
                        // 再根据父子关系规则，将其所有子权限一并加入
                        for (String candidate : allPermissionCodes) {
                            if (PermissionOverlapHelper.isParentOf(code, candidate)) {
                                resultCodes.add(candidate);
                            }
                        }
                    }
                }
            }

            // 权限来源二：直接分配给角色的权限（与角色管理界面展示口径一致，使其真正生效）
            List<SysPermission> directPermissions = sysRolePermissionMapper.selectPermissionsByRoleId(role.getId());
            for (SysPermission permission : directPermissions) {
                String code = permission.getCode();
                if (!StringUtils.hasText(code)) {
                    continue;
                }
                if (resultCodes.add(code)) {
                    for (String candidate : allPermissionCodes) {
                        if (PermissionOverlapHelper.isParentOf(code, candidate)) {
                            resultCodes.add(candidate);
                        }
                    }
                }
            }
        }

        return new ArrayList<>(resultCodes);
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