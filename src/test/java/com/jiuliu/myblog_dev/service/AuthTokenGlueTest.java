/*
 * [AuthTokenGlueTest.java]
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

package com.jiuliu.myblog_dev.service;

import cloud.tianai.captcha.spring.plugins.secondary.SecondaryVerificationApplication;
import com.jiuliu.myblog_dev.config.business.RsaKeyConfig;
import com.jiuliu.myblog_dev.dto.ServiceResult;
import com.jiuliu.myblog_dev.dto.user.auth.ChangePasswordDTO;
import com.jiuliu.myblog_dev.dto.user.auth.LoginDTO;
import com.jiuliu.myblog_dev.dto.user.manage.UserAdminResponseDTO;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.mapper.config.SysConfigMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserRoleMapper;
import com.jiuliu.myblog_dev.mapper.user.permission.SysPermissionMapper;
import com.jiuliu.myblog_dev.mapper.user.permissionGroup.SysPermissionGroupMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRoleMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRolePermissionMapper;
import com.jiuliu.myblog_dev.security.AuthTokenService;
import com.jiuliu.myblog_dev.security.AuthenticatedUser;
import com.jiuliu.myblog_dev.service.mail.MailService;
import com.jiuliu.myblog_dev.service.user.auth.AuthServiceImpl;
import com.jiuliu.myblog_dev.service.user.manage.UserManageServiceImpl;
import com.jiuliu.myblog_dev.utils.auth.ChangeEmailPendingService;
import com.jiuliu.myblog_dev.utils.auth.OAuthCodeService;
import com.jiuliu.myblog_dev.utils.auth.PendingPasswordResetService;
import com.jiuliu.myblog_dev.utils.auth.RegisterPendingUserService;
import com.jiuliu.myblog_dev.utils.auth.TempLoginTokenService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.jiuliu.myblog_dev.entity.user.SysUserRole;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 登录/登出/改密与用户管理中的令牌衔接测试。
 * 验证业务层签发的令牌确实来自 AuthTokenService，且登出、改密、
 * 禁用、删除都会使既有令牌失效（与迁移前的会话语义一致）。
 */
class AuthTokenGlueTest {

    @BeforeAll
    static void initMybatisPlusMetadata() {
        // 纯单元测试无 Spring/MyBatis 上下文，手动注册 lambda 方法引用所需的实体元数据
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysUser.class);
        TableInfoHelper.initTableInfo(assistant, SysUserRole.class);
    }

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthTokenService authTokenService;
    private SysUserMapper sysUserMapper;
    private AuthServiceImpl authService;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        authTokenService = new AuthTokenService(3600);
        sysUserMapper = mock(SysUserMapper.class);
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        RsaKeyConfig rsaKeyConfig = mock(RsaKeyConfig.class);
        when(rsaKeyConfig.getPrivateKeyBase64())
                .thenReturn(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
        when(rsaKeyConfig.getPublicKeyBase64())
                .thenReturn(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));

        TempLoginTokenService tempLoginTokenService = mock(TempLoginTokenService.class);
        when(tempLoginTokenService.consumeToken(anyString())).thenReturn("unbound");
        SecondaryVerificationApplication captcha = mock(SecondaryVerificationApplication.class);
        when(captcha.secondaryVerification(anyString())).thenReturn(true);
        OAuthCodeService oauthCodeService = mock(OAuthCodeService.class);
        when(oauthCodeService.generateCode(eq(11L), anyString())).thenReturn("oauth-code-1");

        authService = new AuthServiceImpl(sysUserMapper, mock(SysUserRoleMapper.class),
                mock(SysRoleMapper.class), mock(SysPermissionMapper.class),
                mock(SysPermissionGroupMapper.class), mock(SysRolePermissionMapper.class),
                mock(SysConfigMapper.class), mock(RegisterPendingUserService.class),
                mock(ChangeEmailPendingService.class), rsaKeyConfig, passwordEncoder,
                tempLoginTokenService, oauthCodeService, captcha,
                mock(MailService.class), mock(PendingPasswordResetService.class), authTokenService);
        ReflectionTestUtils.setField(authService, "activeTimeoutRemember", 7200L);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private SysUser user(long id, String rawPassword) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("testuser");
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setStatus(1);
        user.setIsDeleted(0);
        return user;
    }

    private String encrypt(String plain) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
        return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
    }

    private LoginDTO loginDto(boolean rememberMe, boolean oauth) throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("testuser");
        dto.setPassword(encrypt("Secret123"));
        dto.setTempToken("temp");
        dto.setCaptchaVerification("captcha-ok");
        dto.setRememberMe(rememberMe);
        dto.setOauthEnabled(oauth);
        return dto;
    }

    @SuppressWarnings("unchecked")
    private String tokenOf(ServiceResult result) {
        return (String) ((Map<String, Object>) result.getData()).get("token");
    }

    @Test
    void loginIssuesResolvableTokenAndRejectsWrongPassword() throws Exception {
        when(sysUserMapper.selectOne(any())).thenReturn(user(11L, "Secret123"));
        ServiceResult ok = authService.login(loginDto(false, false));
        assertThat(ok.getCode()).isEqualTo(200);
        assertThat(authTokenService.resolveUserId(tokenOf(ok))).contains(11L);

        LoginDTO wrong = loginDto(false, false);
        wrong.setPassword(encrypt("Wrong12345"));
        ServiceResult bad = authService.login(wrong);
        assertThat(bad.getCode()).isEqualTo(400);
    }

    @Test
    void oauthLoginReturnsCodeAndSameToken() throws Exception {
        when(sysUserMapper.selectOne(any())).thenReturn(user(11L, "Secret123"));
        ServiceResult result = authService.login(loginDto(true, true));
        assertThat(result.getCode()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertThat(data.get("code")).isEqualTo("oauth-code-1");
        assertThat(authTokenService.resolveUserId((String) data.get("token"))).contains(11L);
    }

    @Test
    void logoutInvalidatesCurrentTokenAndAnonymousLogoutIs401() throws Exception {
        when(sysUserMapper.selectOne(any())).thenReturn(user(11L, "Secret123"));
        String token = tokenOf(authService.login(loginDto(true, false)));
        var principal = new AuthenticatedUser(11L, "testuser", "x", true, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, token, List.of()));

        assertThat(authService.logout().getCode()).isEqualTo(200);
        assertThat(authTokenService.resolveUserId(token)).isEmpty();

        SecurityContextHolder.clearContext();
        assertThat(authService.logout().getCode()).isEqualTo(401);
    }

    @Test
    void updatePasswordInvalidatesAllUserTokens() throws Exception {
        SysUser user = user(11L, "OldPass123!");
        when(sysUserMapper.selectById(11L)).thenReturn(user);
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);
        String token = authTokenService.login(11L, -1);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOld_password(encrypt("OldPass123!"));
        dto.setNew_password(encrypt("NewPass123!"));
        ServiceResult result = authService.updatePassword(dto, 11L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(authTokenService.resolveUserId(token)).isEmpty();
        assertThat(passwordEncoder.matches("NewPass123!", user.getPassword())).isTrue();
    }

    // ---------- UserManageServiceImpl 的强制下线衔接 ----------

    private UserManageServiceImpl userManage(SysUserMapper userMapper, SysRoleMapper roleMapper) {
        when(roleMapper.selectRolesByUserId(any(Long.class))).thenReturn(List.of());
        return new UserManageServiceImpl(userMapper, mock(SysUserRoleMapper.class), roleMapper, authTokenService);
    }

    @Test
    void disablingOrDeletingUserForcesLogoutButEnablingDoesNot() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUser bob = user(7L, "Secret123");
        bob.setUsername("bobuser");
        when(userMapper.selectOne(any())).thenReturn(bob);
        when(userMapper.update(org.mockito.ArgumentMatchers.isNull(), any())).thenReturn(1);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        when(userRoleMapper.delete(any())).thenReturn(1);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        when(roleMapper.selectRolesByUserId(any(Long.class))).thenReturn(List.of());
        UserManageServiceImpl service = new UserManageServiceImpl(
                userMapper, userRoleMapper, roleMapper, authTokenService);

        String token = authTokenService.login(7L, -1);
        assertThat(service.updateUserStatus(7L, 0).getCode()).isEqualTo(200);
        assertThat(authTokenService.resolveUserId(token)).isEmpty();

        String token2 = authTokenService.login(7L, -1);
        assertThat(service.updateUserStatus(7L, 1).getCode()).isEqualTo(200);
        assertThat(authTokenService.resolveUserId(token2)).contains(7L);

        assertThat(service.deleteUser(7L).getCode()).isEqualTo(200);
        assertThat(authTokenService.resolveUserId(token2)).isEmpty();
    }

    @Test
    void userDetailReflectsLoginState() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUser bob = user(7L, "Secret123");
        bob.setUsername("bobuser");
        when(userMapper.selectOne(any())).thenReturn(bob);
        UserManageServiceImpl service = userManage(userMapper, mock(SysRoleMapper.class));

        authTokenService.login(7L, -1);
        ServiceResult loggedIn = service.getUserById(7L);
        assertThat(((UserAdminResponseDTO) loggedIn.getData()).getIsLoggedIn()).isTrue();

        authTokenService.logoutUser(7L);
        ServiceResult loggedOut = service.getUserById(7L);
        assertThat(((UserAdminResponseDTO) loggedOut.getData()).getIsLoggedIn()).isFalse();
    }
}
