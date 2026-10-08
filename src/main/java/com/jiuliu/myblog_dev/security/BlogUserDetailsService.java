/*
 * [BlogUserDetailsService.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 */

package com.jiuliu.myblog_dev.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.entity.user.permission.SysPermission;
import com.jiuliu.myblog_dev.entity.user.permissiongroup.SysPermissionGroup;
import com.jiuliu.myblog_dev.entity.user.role.SysRole;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.mapper.user.permission.SysPermissionMapper;
import com.jiuliu.myblog_dev.mapper.user.permissionGroup.SysPermissionGroupMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRoleMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRolePermissionMapper;
import com.jiuliu.myblog_dev.utils.security.PermissionOverlapHelper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security 用户与权限加载服务。
 * 数据来源与展开规则不变：sys_user / sys_user_role / sys_role /
 * sys_role_permission / sys_role_permission_group / sys_permission_group_item，
 * 父权限自动展开子权限，超级管理员拥有全部权限。
 */
@Service
public class BlogUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysPermissionGroupMapper sysPermissionGroupMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;

    public BlogUserDetailsService(SysUserMapper sysUserMapper,
                                  SysRoleMapper sysRoleMapper,
                                  SysPermissionMapper sysPermissionMapper,
                                  SysPermissionGroupMapper sysPermissionGroupMapper,
                                  SysRolePermissionMapper sysRolePermissionMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysPermissionGroupMapper = sysPermissionGroupMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
    }

    @Override
    public AuthenticatedUser loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(new QueryWrapper<SysUser>()
                .eq("username", username)
                .eq("is_deleted", 0));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return toAuthenticatedUser(user);
    }

    public AuthenticatedUser loadUserById(Long userId) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            throw new UsernameNotFoundException("用户不存在: " + userId);
        }
        return toAuthenticatedUser(user);
    }

    private AuthenticatedUser toAuthenticatedUser(SysUser user) {
        boolean enabled = user.getStatus() != null && user.getStatus() == 1;
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getPassword(),
                enabled, buildAuthorities(user.getId()));
    }

    /** 角色编码列表（与原 StpInterface.getRoleList 口径一致）。 */
    public List<String> getRoleList(Long userId) {
        List<SysRole> roleList = sysRoleMapper.selectRolesByUserId(userId);
        return roleList.stream().map(SysRole::getCode).collect(Collectors.toList());
    }

    /** 权限编码列表（与原 StpInterface.getPermissionList 口径一致，含父权限展开）。 */
    public List<String> getPermissionList(Long userId) {
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
                    addWithChildren(resultCodes, permission.getCode(), allPermissionCodes);
                }
            }

            // 权限来源二：直接分配给角色的权限
            List<SysPermission> directPermissions = sysRolePermissionMapper.selectPermissionsByRoleId(role.getId());
            for (SysPermission permission : directPermissions) {
                addWithChildren(resultCodes, permission.getCode(), allPermissionCodes);
            }
        }

        return new ArrayList<>(resultCodes);
    }

    private void addWithChildren(Set<String> resultCodes, String code, List<String> allPermissionCodes) {
        if (!StringUtils.hasText(code)) {
            return;
        }
        // 先加入自身权限，再根据父子关系规则，将其所有子权限一并加入
        if (resultCodes.add(code)) {
            for (String candidate : allPermissionCodes) {
                if (PermissionOverlapHelper.isParentOf(code, candidate)) {
                    resultCodes.add(candidate);
                }
            }
        }
    }

    private List<GrantedAuthority> buildAuthorities(Long userId) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (String roleCode : getRoleList(userId)) {
            if (StringUtils.hasText(roleCode)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode));
            }
        }
        for (String permissionCode : getPermissionList(userId)) {
            authorities.add(new SimpleGrantedAuthority(permissionCode));
        }
        return new ArrayList<>(authorities);
    }
}
