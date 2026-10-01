package com.jiuliu.myblog_dev.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.service.authentication.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BlogUserDetailsServiceTest {

    private SysUserMapper userMapper;
    private AuthenticationService authenticationService;
    private BlogUserDetailsService service;

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        authenticationService = mock(AuthenticationService.class);
        service = new BlogUserDetailsService(userMapper, authenticationService);
    }

    private SysUser user(long id, int status, int deleted) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("alice");
        user.setPassword("bcrypt-hash");
        user.setStatus(status);
        user.setIsDeleted(deleted);
        return user;
    }

    private List<String> authorityNames(BlogUserDetails details) {
        return details.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    void buildsAuthoritiesWithRolePrefixAndPermissions() {
        when(userMapper.selectById(1L)).thenReturn(user(1L, 1, 0));
        when(authenticationService.getRoleList(1L)).thenReturn(List.of("AUTHOR"));
        when(authenticationService.getPermissionList(1L)).thenReturn(List.of("article:create", "article:list"));

        BlogUserDetails details = service.loadUserById(1L);

        assertEquals(List.of("ROLE_AUTHOR", "article:create", "article:list"), authorityNames(details));
        assertTrue(details.isEnabled());
        assertEquals("alice", details.getUsername());
    }

    @Test
    void disabledUserIsNotEnabled() {
        when(userMapper.selectById(2L)).thenReturn(user(2L, 0, 0));
        when(authenticationService.getRoleList(2L)).thenReturn(List.of());
        when(authenticationService.getPermissionList(2L)).thenReturn(List.of());

        assertFalse(service.loadUserById(2L).isEnabled());
    }

    @Test
    void deletedOrMissingUserByIdReturnsNull() {
        when(userMapper.selectById(3L)).thenReturn(user(3L, 1, 1));
        assertNull(service.loadUserById(3L));
        assertNull(service.loadUserById(99L));
        assertNull(service.loadUserById(null));
    }

    @Test
    void loadByUsernameFindsUserAndRejectsUnknown() {
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user(1L, 1, 0));
        when(authenticationService.getRoleList(1L)).thenReturn(List.of("USER"));
        when(authenticationService.getPermissionList(1L)).thenReturn(List.of());

        assertEquals(1L, service.loadUserByUsername("alice").getUserId());

        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("ghost"));
    }
}
