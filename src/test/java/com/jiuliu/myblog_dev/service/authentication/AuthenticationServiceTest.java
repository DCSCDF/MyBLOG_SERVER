package com.jiuliu.myblog_dev.service.authentication;

import com.jiuliu.myblog_dev.entity.user.permission.SysPermission;
import com.jiuliu.myblog_dev.entity.user.permissiongroup.SysPermissionGroup;
import com.jiuliu.myblog_dev.entity.user.role.SysRole;
import com.jiuliu.myblog_dev.mapper.user.permission.SysPermissionMapper;
import com.jiuliu.myblog_dev.mapper.user.permissionGroup.SysPermissionGroupMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRoleMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRolePermissionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {

    private SysRoleMapper roleMapper;
    private SysPermissionMapper permissionMapper;
    private SysPermissionGroupMapper groupMapper;
    private SysRolePermissionMapper rolePermissionMapper;
    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        roleMapper = mock(SysRoleMapper.class);
        permissionMapper = mock(SysPermissionMapper.class);
        groupMapper = mock(SysPermissionGroupMapper.class);
        rolePermissionMapper = mock(SysRolePermissionMapper.class);
        service = new AuthenticationService(roleMapper, permissionMapper, groupMapper, rolePermissionMapper);
    }

    private SysRole role(long id, String code, boolean superAdmin) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setCode(code);
        role.setSuperAdmin(superAdmin);
        return role;
    }

    private SysPermission permission(String code) {
        SysPermission permission = new SysPermission();
        permission.setCode(code);
        return permission;
    }

    @Test
    void roleListReturnsRoleCodes() {
        when(roleMapper.selectRolesByUserId(1L))
                .thenReturn(List.of(role(1L, "USER", false), role(2L, "AUTHOR", false)));

        assertEquals(List.of("USER", "AUTHOR"), service.getRoleList(1L));
    }

    @Test
    void parentPermissionExpandsToAllChildren() {
        when(roleMapper.selectRolesByUserId(1L)).thenReturn(List.of(role(2L, "AUTHOR", false)));
        when(permissionMapper.selectList(null)).thenReturn(List.of(
                permission("article"), permission("article:create"),
                permission("article:list"), permission("comment:list")));
        when(groupMapper.selectGroupsByRoleId(2L)).thenReturn(List.of());
        when(rolePermissionMapper.selectPermissionsByRoleId(2L))
                .thenReturn(List.of(permission("article")));

        List<String> result = service.getPermissionList(1L);

        assertTrue(result.containsAll(List.of("article", "article:create", "article:list")));
        assertEquals(3, result.size());
    }

    @Test
    void permissionsFromGroupsAreIncludedAndExpanded() {
        SysPermissionGroup group = new SysPermissionGroup();
        group.setId(5L);
        when(roleMapper.selectRolesByUserId(1L)).thenReturn(List.of(role(2L, "AUTHOR", false)));
        when(permissionMapper.selectList(null)).thenReturn(List.of(
                permission("system:user"), permission("system:user:list")));
        when(groupMapper.selectGroupsByRoleId(2L)).thenReturn(List.of(group));
        when(permissionMapper.selectPermissionsByGroupId(5L))
                .thenReturn(List.of(permission("system:user")));
        when(rolePermissionMapper.selectPermissionsByRoleId(2L)).thenReturn(List.of());

        List<String> result = service.getPermissionList(1L);

        assertTrue(result.containsAll(List.of("system:user", "system:user:list")));
    }

    @Test
    void superAdminReceivesEveryRegisteredPermission() {
        when(roleMapper.selectRolesByUserId(1L)).thenReturn(List.of(role(1L, "SUPER_ADMIN", true)));
        when(permissionMapper.selectList(null)).thenReturn(List.of(
                permission("article:create"), permission("system:user:list")));

        assertEquals(List.of("article:create", "system:user:list"), service.getPermissionList(1L));
    }
}
