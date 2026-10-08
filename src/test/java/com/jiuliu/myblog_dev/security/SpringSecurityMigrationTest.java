/*
 * [SpringSecurityMigrationTest.java]
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

import com.jiuliu.myblog_dev.dto.ServiceResult;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.entity.user.permission.SysPermission;
import com.jiuliu.myblog_dev.entity.user.permissiongroup.SysPermissionGroup;
import com.jiuliu.myblog_dev.entity.user.role.SysRole;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.mapper.user.permission.SysPermissionMapper;
import com.jiuliu.myblog_dev.mapper.user.permissionGroup.SysPermissionGroupMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRoleMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRolePermissionMapper;
import com.jiuliu.myblog_dev.utils.security.RandomStringUtil;
import com.jiuliu.myblog_dev.utils.security.SecurityUtils;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Spring Security 鉴权迁移的回归测试。
 * 覆盖：服务结果语义、令牌单端登录与过期、临时存储过期、
 * 权限展开（权限组 + 直授 + 父权限展开 + 超管）、令牌过滤器、
 * SecurityUtils、@PreAuthorize 的 hasAuthority/hasRole 口径。
 */
class SpringSecurityMigrationTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------- ServiceResult（原 ServiceResult 语义） ----------

    @Test
    void serviceResultSemanticsPreserved() {
        ServiceResult data = ServiceResult.data("x");
        assertThat(data.getCode()).isEqualTo(200);
        assertThat(data.getData()).isEqualTo("x");

        ServiceResult error = ServiceResult.error("bad").setCode(400);
        assertThat(error.getCode()).isEqualTo(400);
        assertThat(error.getMsg()).isEqualTo("bad");

        assertThat(ServiceResult.ok("done").getMsg()).isEqualTo("done");
        assertThat(ServiceResult.ok().setMsg("删除成功").getMsg()).isEqualTo("删除成功");
    }

    // ---------- AuthTokenService ----------

    @Test
    void newLoginInvalidatesPreviousToken() {
        AuthTokenService tokens = new AuthTokenService(3600);
        String first = tokens.login(7L, -1);
        assertThat(tokens.resolveUserId(first)).contains(7L);

        String second = tokens.login(7L, -1);
        assertThat(second).isNotEqualTo(first);
        assertThat(tokens.resolveUserId(first)).isEmpty();
        assertThat(tokens.resolveUserId(second)).contains(7L);
        assertThat(tokens.isLoggedIn(7L)).isTrue();

        tokens.logout(second);
        assertThat(tokens.resolveUserId(second)).isEmpty();
        assertThat(tokens.isLoggedIn(7L)).isFalse();
    }

    @Test
    void logoutUserAndAbsoluteExpiry() throws Exception {
        AuthTokenService tokens = new AuthTokenService(3600);
        String token = tokens.login(9L, -1);
        tokens.logoutUser(9L);
        assertThat(tokens.resolveUserId(token)).isEmpty();

        AuthTokenService shortLived = new AuthTokenService(1);
        String expiring = shortLived.login(9L, -1);
        Thread.sleep(1100);
        assertThat(shortLived.resolveUserId(expiring)).isEmpty();
    }

    // ---------- ExpiringValueStore ----------

    @Test
    void expiringValueStoreHonoursTtl() throws Exception {
        ExpiringValueStore store = new ExpiringValueStore();
        store.set("k", "v", 1);
        assertThat(store.get("k")).isEqualTo("v");
        Thread.sleep(1100);
        assertThat(store.get("k")).isNull();
        store.set("k2", "v2", 60);
        store.delete("k2");
        assertThat(store.get("k2")).isNull();
    }

    // ---------- BlogUserDetailsService（权限展开） ----------

    private SysPermission permission(String code) {
        SysPermission permission = new SysPermission();
        permission.setCode(code);
        return permission;
    }

    private SysRole role(long id, String code, boolean superAdmin) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setCode(code);
        role.setSuperAdmin(superAdmin);
        return role;
    }

    private BlogUserDetailsService userDetailsService(SysRole role) {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysPermissionMapper permissionMapper = mock(SysPermissionMapper.class);
        SysPermissionGroupMapper groupMapper = mock(SysPermissionGroupMapper.class);
        SysRolePermissionMapper rolePermissionMapper = mock(SysRolePermissionMapper.class);

        SysUser user = new SysUser();
        user.setId(7L);
        user.setUsername("editor");
        user.setPassword("encoded");
        user.setStatus(1);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(roleMapper.selectRolesByUserId(7L)).thenReturn(List.of(role));
        when(permissionMapper.selectList(null)).thenReturn(List.of(
                permission("article"), permission("article:create"), permission("article:list"),
                permission("comment:list"), permission("other:x")));
        when(permissionMapper.selectPermissionsByGroupId(10L)).thenReturn(List.of(permission("article")));
        when(rolePermissionMapper.selectPermissionsByRoleId(role.getId()))
                .thenReturn(List.of(permission("comment:list")));
        SysPermissionGroup group = new SysPermissionGroup();
        group.setId(10L);
        when(groupMapper.selectGroupsByRoleId(role.getId())).thenReturn(List.of(group));
        return new BlogUserDetailsService(userMapper, roleMapper, permissionMapper, groupMapper, rolePermissionMapper);
    }

    @Test
    void authoritiesExpandGroupDirectAndParentPermissions() {
        BlogUserDetailsService service = userDetailsService(role(2L, "EDITOR", false));
        AuthenticatedUser user = service.loadUserById(7L);

        Set<String> authorities = user.getAuthorities().stream()
                .map(a -> a.getAuthority()).collect(Collectors.toSet());
        // 角色 + 权限组中的父权限 article 展开为 article:create/article:list + 直授 comment:list
        assertThat(authorities).containsExactlyInAnyOrder(
                "ROLE_EDITOR", "article", "article:create", "article:list", "comment:list");
        assertThat(service.getPermissionList(7L))
                .containsExactlyInAnyOrder("article", "article:create", "article:list", "comment:list");
        assertThat(service.getRoleList(7L)).containsExactly("EDITOR");
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void superAdminGetsAllPermissions() {
        BlogUserDetailsService service = userDetailsService(role(1L, "SUPER_ADMIN", true));
        assertThat(service.getPermissionList(7L)).containsExactlyInAnyOrder(
                "article", "article:create", "article:list", "comment:list", "other:x");
    }

    // ---------- TokenAuthenticationFilter + SecurityUtils ----------

    @Test
    void filterAuthenticatesValidTokenHeaderOnly() throws Exception {
        AuthTokenService tokens = new AuthTokenService(3600);
        String token = tokens.login(7L, -1);
        BlogUserDetailsService userService = mock(BlogUserDetailsService.class);
        when(userService.loadUserById(7L)).thenReturn(new AuthenticatedUser(7L, "editor", "pw", true,
                List.of(new SimpleGrantedAuthority("article:create"), new SimpleGrantedAuthority("ROLE_EDITOR"))));
        TokenAuthenticationFilter filter = new TokenAuthenticationFilter(tokens, userService, "token");
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("token", token);
        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(SecurityUtils.getCurrentUserId()).isEqualTo(7L);
        assertThat(SecurityUtils.getCurrentToken()).isEqualTo(token);
        assertThat(SecurityUtils.isLogin()).isTrue();
        assertThat(SecurityUtils.hasPermission("article:create")).isTrue();
        assertThat(SecurityUtils.hasPermission("article:delete")).isFalse();
        assertThat(SecurityUtils.hasRole("EDITOR")).isTrue();

        SecurityContextHolder.clearContext();
        MockHttpServletRequest anonymous = new MockHttpServletRequest();
        anonymous.addHeader("token", "not-a-real-token");
        filter.doFilterInternal(anonymous, new MockHttpServletResponse(), chain);
        assertThat(SecurityUtils.isLogin()).isFalse();
        assertThatThrownBy(SecurityUtils::getCurrentUserId)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    // ---------- @PreAuthorize 口径（hasAuthority / hasRole） ----------

    public static class SecuredBean {
        @PreAuthorize("hasAuthority('article:create')")
        public String create() {
            return "created";
        }

        @PreAuthorize("hasRole('SUPER_ADMIN')")
        public String admin() {
            return "admin";
        }
    }

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
        @Bean
        SecuredBean securedBean() {
            return new SecuredBean();
        }
    }

    @Test
    void preAuthorizeMatchesPermissionAndRoleCodes() {
        try (var context = new AnnotationConfigApplicationContext(MethodSecurityConfig.class)) {
            SecuredBean bean = context.getBean(SecuredBean.class);

            var editor = new AuthenticatedUser(7L, "editor", "pw", true, List.of(
                    new SimpleGrantedAuthority("article:create"), new SimpleGrantedAuthority("ROLE_EDITOR")));
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(editor, null, editor.getAuthorities()));
            assertThat(bean.create()).isEqualTo("created");
            assertThatThrownBy(bean::admin).isInstanceOf(AccessDeniedException.class);

            var admin = new AuthenticatedUser(1L, "root", "pw", true, List.of(
                    new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));
            assertThat(bean.admin()).isEqualTo("admin");
            assertThatThrownBy(bean::create).isInstanceOf(AccessDeniedException.class);
        }
    }

    // ---------- 补充覆盖：随机串 / 按用户名加载 / 禁用用户 / Bearer / 活跃超时 ----------

    @Test
    void randomStringHasRequestedLengthAndCharset() {
        String value = RandomStringUtil.random(128);
        assertThat(value).hasSize(128);
        assertThat(value).matches("[a-zA-Z0-9]+");
        assertThat(RandomStringUtil.random(32)).isNotEqualTo(RandomStringUtil.random(32));
    }

    @Test
    void loadUserByUsernameFoundAndMissing() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysPermissionMapper permissionMapper = mock(SysPermissionMapper.class);
        when(roleMapper.selectRolesByUserId(org.mockito.ArgumentMatchers.anyLong())).thenReturn(List.of());
        when(permissionMapper.selectList(null)).thenReturn(List.of());
        BlogUserDetailsService service = new BlogUserDetailsService(userMapper,
                roleMapper, permissionMapper,
                mock(SysPermissionGroupMapper.class), mock(SysRolePermissionMapper.class));
        SysUser user = new SysUser();
        user.setId(3L);
        user.setUsername("alice");
        user.setPassword("encoded");
        user.setStatus(1);
        when(userMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(user);
        assertThat(service.loadUserByUsername("alice").getId()).isEqualTo(3L);

        when(userMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(null);
        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void disabledUserIsNotAuthenticatedAndBearerHeaderWorks() throws Exception {
        AuthTokenService tokens = new AuthTokenService(3600);
        String token = tokens.login(8L, -1);
        BlogUserDetailsService userService = mock(BlogUserDetailsService.class);
        TokenAuthenticationFilter filter = new TokenAuthenticationFilter(tokens, userService, "token");
        FilterChain chain = mock(FilterChain.class);

        // 禁用用户：即使令牌有效也不建立认证
        when(userService.loadUserById(8L)).thenReturn(new AuthenticatedUser(8L, "off", "pw", false,
                List.of(new SimpleGrantedAuthority("article:create"))));
        MockHttpServletRequest disabledReq = new MockHttpServletRequest();
        disabledReq.addHeader("token", token);
        filter.doFilterInternal(disabledReq, new MockHttpServletResponse(), chain);
        assertThat(SecurityUtils.isLogin()).isFalse();

        // 启用用户 + Authorization: Bearer 头同样可认证
        when(userService.loadUserById(8L)).thenReturn(new AuthenticatedUser(8L, "on", "pw", true,
                List.of(new SimpleGrantedAuthority("article:create"))));
        MockHttpServletRequest bearerReq = new MockHttpServletRequest();
        bearerReq.addHeader("Authorization", "Bearer " + token);
        filter.doFilterInternal(bearerReq, new MockHttpServletResponse(), chain);
        assertThat(SecurityUtils.getCurrentUserId()).isEqualTo(8L);
    }

    @Test
    void activeTimeoutExpiresIdleSession() throws Exception {
        AuthTokenService tokens = new AuthTokenService(3600);
        String token = tokens.login(5L, 1);
        assertThat(tokens.resolveUserId(token)).contains(5L);
        Thread.sleep(1100);
        assertThat(tokens.resolveUserId(token)).isEmpty();
    }

    @Test
    void securityUtilsAnonymousDefaults() {
        assertThat(SecurityUtils.isLogin()).isFalse();
        assertThat(SecurityUtils.getCurrentUserIdOrNull()).isNull();
        assertThat(SecurityUtils.getCurrentUserOrNull()).isNull();
        assertThat(SecurityUtils.getCurrentToken()).isNull();
        assertThat(SecurityUtils.hasPermission("article:create")).isFalse();
        assertThat(SecurityUtils.hasRole("SUPER_ADMIN")).isFalse();
    }
}
