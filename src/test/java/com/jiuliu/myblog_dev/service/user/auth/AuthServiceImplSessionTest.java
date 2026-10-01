package com.jiuliu.myblog_dev.service.user.auth;

import cloud.tianai.captcha.spring.plugins.secondary.SecondaryVerificationApplication;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuliu.myblog_dev.common.ServiceResult;
import com.jiuliu.myblog_dev.common.TtlStore;
import com.jiuliu.myblog_dev.config.business.RsaKeyConfig;
import com.jiuliu.myblog_dev.dto.user.auth.ChangePasswordDTO;
import com.jiuliu.myblog_dev.dto.user.auth.LoginDTO;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.mapper.config.SysConfigMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserRoleMapper;
import com.jiuliu.myblog_dev.mapper.user.permission.SysPermissionMapper;
import com.jiuliu.myblog_dev.mapper.user.permissionGroup.SysPermissionGroupMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRoleMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRolePermissionMapper;
import com.jiuliu.myblog_dev.security.ActiveTokenRegistry;
import com.jiuliu.myblog_dev.security.BlogUserDetails;
import com.jiuliu.myblog_dev.security.JwtService;
import com.jiuliu.myblog_dev.service.mail.MailService;
import com.jiuliu.myblog_dev.utils.auth.ChangeEmailPendingService;
import com.jiuliu.myblog_dev.utils.auth.OAuthCodeService;
import com.jiuliu.myblog_dev.utils.auth.PendingPasswordResetService;
import com.jiuliu.myblog_dev.utils.auth.RegisterPendingUserService;
import com.jiuliu.myblog_dev.utils.auth.TempLoginTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Session behavior of the migrated login flow: a successful login issues a
 * JWT and revokes the user's previous tokens (single-session), bad
 * credentials/disabled accounts issue nothing, logout revokes the current
 * token, and a password change revokes every token of the user.
 */
class AuthServiceImplSessionTest {

    private static final String RAW_PASSWORD = "Pass1234";

    private SysUserMapper userMapper;
    private RsaKeyConfig rsaKeyConfig;
    private BCryptPasswordEncoder passwordEncoder;
    private TempLoginTokenService tempLoginTokenService;
    private TtlStore ttlStore;
    private JwtService jwtService;
    private ActiveTokenRegistry tokenRegistry;
    private AuthServiceImpl authService;
    private SysUser user;

    @BeforeEach
    void setUp() throws Exception {
        userMapper = mock(SysUserMapper.class);
        rsaKeyConfig = new RsaKeyConfig();
        rsaKeyConfig.init();
        passwordEncoder = new BCryptPasswordEncoder();
        ttlStore = new TtlStore();
        tempLoginTokenService = new TempLoginTokenService(ttlStore);
        tokenRegistry = new ActiveTokenRegistry();
        jwtService = new JwtService("test-only-secret-key-for-myblog-tests-0123456789",
                "Myblog_dev", 3600, 3600, tokenRegistry);

        SecondaryVerificationApplication captcha = mock(SecondaryVerificationApplication.class);
        when(captcha.secondaryVerification(anyString())).thenReturn(true);

        authService = new AuthServiceImpl(
                userMapper,
                mock(SysUserRoleMapper.class),
                mock(SysRoleMapper.class),
                mock(SysPermissionMapper.class),
                mock(SysPermissionGroupMapper.class),
                mock(SysRolePermissionMapper.class),
                mock(SysConfigMapper.class),
                mock(RegisterPendingUserService.class),
                mock(ChangeEmailPendingService.class),
                rsaKeyConfig,
                passwordEncoder,
                tempLoginTokenService,
                new OAuthCodeService(ttlStore),
                captcha,
                mock(MailService.class),
                mock(PendingPasswordResetService.class),
                jwtService,
                tokenRegistry);

        user = new SysUser();
        user.setId(1L);
        user.setUsername("alice01");
        user.setPassword(passwordEncoder.encode(RAW_PASSWORD));
        user.setStatus(1);
        user.setIsDeleted(0);
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);
        when(userMapper.selectById(1L)).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        tempLoginTokenService.destroy();
        ttlStore.destroy();
        rsaKeyConfig.destroy();
    }

    private String encrypt(String raw) throws Exception {
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(rsaKeyConfig.getPublicKeyBase64())));
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private LoginDTO loginDto(String rawPassword, String tempToken) throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("alice01");
        dto.setPassword(encrypt(rawPassword));
        dto.setTempToken(tempToken);
        dto.setCaptchaVerification("captcha-ok");
        dto.setRememberMe(false);
        return dto;
    }

    @Test
    void successfulLoginIssuesJwtAndRevokesPreviousTokens() throws Exception {
        String previous = jwtService.generateToken(1L, "alice01", false);
        String previousJti = jwtService.parse(previous).jti();

        ServiceResult result = authService.login(
                loginDto(RAW_PASSWORD, tempLoginTokenService.generateTempToken()));

        assertEquals(200, result.getCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        String issued = (String) data.get("token");
        assertEquals(1L, jwtService.parse(issued).userId());
        assertTrue(tokenRegistry.isDenied(previousJti));
        assertFalse(tokenRegistry.isDenied(jwtService.parse(issued).jti()));
    }

    @Test
    void wrongPasswordIssuesNoToken() throws Exception {
        ServiceResult result = authService.login(
                loginDto("Wrong123", tempLoginTokenService.generateTempToken()));

        assertEquals(400, result.getCode());
        assertFalse(tokenRegistry.isLoggedIn(1L));
    }

    @Test
    void disabledUserCannotLogin() throws Exception {
        user.setStatus(0);

        ServiceResult result = authService.login(
                loginDto(RAW_PASSWORD, tempLoginTokenService.generateTempToken()));

        assertEquals(403, result.getCode());
        assertFalse(tokenRegistry.isLoggedIn(1L));
    }

    @Test
    void unknownTempTokenRejectsLogin() throws Exception {
        ServiceResult result = authService.login(loginDto(RAW_PASSWORD, "never-issued"));

        assertEquals(400, result.getCode());
        assertFalse(tokenRegistry.isLoggedIn(1L));
    }

    @Test
    void logoutRevokesCurrentTokenAndAnonymousLogoutFails() {
        String token = jwtService.generateToken(1L, "alice01", false);
        String jti = jwtService.parse(token).jti();
        BlogUserDetails principal = new BlogUserDetails(1L, "alice01", "x", true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        auth.setDetails(jti);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertEquals(200, authService.logout().getCode());
        assertTrue(tokenRegistry.isDenied(jti));

        SecurityContextHolder.clearContext();
        assertEquals(401, authService.logout().getCode());
    }

    @Test
    void passwordChangeRevokesAllUserTokens() throws Exception {
        String token = jwtService.generateToken(1L, "alice01", false);
        String jti = jwtService.parse(token).jti();
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOld_password(encrypt(RAW_PASSWORD));
        dto.setNew_password(encrypt("NewPass123"));

        assertEquals(200, authService.updatePassword(dto, 1L).getCode());
        assertTrue(tokenRegistry.isDenied(jti));
        assertFalse(tokenRegistry.isLoggedIn(1L));
    }
}
