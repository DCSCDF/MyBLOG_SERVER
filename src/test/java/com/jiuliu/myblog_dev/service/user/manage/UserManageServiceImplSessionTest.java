package com.jiuliu.myblog_dev.service.user.manage;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuliu.myblog_dev.common.ServiceResult;
import com.jiuliu.myblog_dev.dto.user.manage.PageUserDTO;
import com.jiuliu.myblog_dev.dto.user.manage.PageUserResponseDTO;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserRoleMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRoleMapper;
import com.jiuliu.myblog_dev.security.ActiveTokenRegistry;
import com.jiuliu.myblog_dev.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Session side effects of user administration after the Spring Security
 * migration: disabling or deleting a user force-logs them out (all JWTs
 * revoked), enabling does not, and the admin list reports live login state
 * from the token registry.
 */
class UserManageServiceImplSessionTest {

    private SysUserMapper userMapper;
    private SysRoleMapper roleMapper;
    private ActiveTokenRegistry tokenRegistry;
    private JwtService jwtService;
    private UserManageServiceImpl service;
    private SysUser user;

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        roleMapper = mock(SysRoleMapper.class);
        tokenRegistry = new ActiveTokenRegistry();
        jwtService = new JwtService("test-only-secret-key-for-myblog-tests-0123456789",
                "Myblog_dev", 3600, 3600, tokenRegistry);
        service = new UserManageServiceImpl(userMapper, mock(SysUserRoleMapper.class),
                roleMapper, tokenRegistry);

        user = new SysUser();
        user.setId(5L);
        user.setUsername("victim01");
        user.setStatus(1);
        user.setIsDeleted(0);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(roleMapper.selectRolesByUserId(5L)).thenReturn(List.of());
    }

    @Test
    void disablingUserRevokesAllTokens() {
        String token = jwtService.generateToken(5L, "victim01", false);
        String jti = jwtService.parse(token).jti();

        ServiceResult result = service.updateUserStatus(5L, 0);

        assertEquals(200, result.getCode());
        assertTrue(tokenRegistry.isDenied(jti));
        assertFalse(tokenRegistry.isLoggedIn(5L));
    }

    @Test
    void enablingUserKeepsTokens() {
        String token = jwtService.generateToken(5L, "victim01", false);

        assertEquals(200, service.updateUserStatus(5L, 1).getCode());
        assertFalse(tokenRegistry.isDenied(jwtService.parse(token).jti()));
        assertTrue(tokenRegistry.isLoggedIn(5L));
    }

    @Test
    void deletingUserRevokesAllTokens() {
        String token = jwtService.generateToken(5L, "victim01", false);
        String jti = jwtService.parse(token).jti();

        assertEquals(200, service.deleteUser(5L).getCode());
        assertTrue(tokenRegistry.isDenied(jti));
    }

    @Test
    void userListReportsLoggedInFromRegistry() {
        jwtService.generateToken(5L, "victim01", false);
        Page<SysUser> page = new Page<>(1, 10);
        page.setRecords(List.of(user));
        page.setTotal(1);
        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageUserDTO dto = new PageUserDTO();
        dto.setCurrentPage(1);
        dto.setPageSize(10);
        ServiceResult result = service.getPageUsers(dto);

        assertEquals(200, result.getCode());
        PageUserResponseDTO data = (PageUserResponseDTO) result.getData();
        assertEquals(Boolean.TRUE, data.getRecords().get(0).getIsLoggedIn());
    }
}
