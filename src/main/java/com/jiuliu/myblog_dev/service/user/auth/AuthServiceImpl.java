/*
 * [AuthServiceImpl.java]
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

package com.jiuliu.myblog_dev.service.user.auth;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.spring.plugins.secondary.SecondaryVerificationApplication;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jiuliu.myblog_dev.config.business.RsaKeyConfig;
import com.jiuliu.myblog_dev.dto.user.UserResponseDTO;
import com.jiuliu.myblog_dev.dto.user.auth.*;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.entity.user.SysUserRole;
import com.jiuliu.myblog_dev.entity.user.permission.SysPermission;
import com.jiuliu.myblog_dev.entity.user.permissiongroup.SysPermissionGroup;
import com.jiuliu.myblog_dev.entity.user.role.SysRole;
import com.jiuliu.myblog_dev.mapper.config.SysConfigMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserRoleMapper;
import com.jiuliu.myblog_dev.mapper.user.permission.SysPermissionMapper;
import com.jiuliu.myblog_dev.mapper.user.permissionGroup.SysPermissionGroupMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRoleMapper;
import com.jiuliu.myblog_dev.service.mail.MailService;
import com.jiuliu.myblog_dev.utils.auth.OAuthCodeService;
import com.jiuliu.myblog_dev.utils.auth.RegisterPendingUserService;
import com.jiuliu.myblog_dev.utils.auth.TempLoginTokenService;
import com.jiuliu.myblog_dev.utils.rsa.RsaUtils;
import com.jiuliu.myblog_dev.utils.security.PermissionOverlapHelper;
import com.jiuliu.myblog_dev.utils.validation.ValidationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.Random;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final String CONFIG_KEY_REGISTER_DEFAULT_ROLE = "user_register_default_role";
    private static final String CONFIG_KEY_REG_USE_EMAIL = "reg.use-email";
    private static final int REGISTER_CODE_LENGTH = 6;
    private static final int REGISTER_CODE_EXPIRE_MINUTES = 5;

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysPermissionGroupMapper sysPermissionGroupMapper;
    private final SysConfigMapper sysConfigMapper;
    private final RegisterPendingUserService registerPendingUserService;
    private final RsaKeyConfig rsaKeyConfig;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TempLoginTokenService tempLoginTokenService;
    private final OAuthCodeService oauthCodeService;
    private final ImageCaptchaApplication imageCaptchaApplication;
    private final MailService mailService;

    public AuthServiceImpl(
            SysUserMapper sysUserMapper,
            SysUserRoleMapper sysUserRoleMapper,
            SysRoleMapper sysRoleMapper,
            SysPermissionMapper sysPermissionMapper,
            SysPermissionGroupMapper sysPermissionGroupMapper,
            SysConfigMapper sysConfigMapper,
            RegisterPendingUserService registerPendingUserService,
            RsaKeyConfig rsaKeyConfig,
            BCryptPasswordEncoder passwordEncoder,
            TempLoginTokenService tempLoginTokenService,
            OAuthCodeService oauthCodeService,
            ImageCaptchaApplication imageCaptchaApplication,
            MailService mailService) {
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysPermissionGroupMapper = sysPermissionGroupMapper;
        this.sysConfigMapper = sysConfigMapper;
        this.registerPendingUserService = registerPendingUserService;
        this.rsaKeyConfig = rsaKeyConfig;
        this.passwordEncoder = passwordEncoder;
        this.tempLoginTokenService = tempLoginTokenService;
        this.oauthCodeService = oauthCodeService;
        this.imageCaptchaApplication = imageCaptchaApplication;
        this.mailService = mailService;
    }

    //    400: '请求参数错误',
    //    401: '未授权，请重新登录',
    //    403: '拒绝访问',
    //    404: '请求的资源不存在',
    //    408: '请求超时',
    //    429: '请求过于频繁',
    //    500: '服务器内部错误',
    //    502: '网关错误',
    //    503: '服务不可用',
    //    504: '网关超时'

    @Override
    public SaResult getPublicKey() {
        Map<String, Object> data = new HashMap<>();
        data.put("publicKey", rsaKeyConfig.getPublicKeyBase64());

        // 生成一个未绑定用户的临时 Token（60秒有效）
        String tempToken = tempLoginTokenService.generateTempToken();
        data.put("tempToken", tempToken);

        return SaResult.data(data);
    }

    @Override
    public SaResult login(LoginDTO dto) {
        // 验证码校验
        SaResult captchaResult = validateCaptcha(dto.getCaptchaVerification(), dto.getUsername());
        if (captchaResult != null) {
            return captchaResult;
        }

        // 校验临时 Token
        String tempToken = dto.getTempToken();
        String tokenValue = tempLoginTokenService.consumeToken(tempToken);

        if (!"unbound".equals(tokenValue)) {
            log.warn("登录失败：临时 Token 无效或已过期，token={}", tempToken);
            return SaResult.error("临时登录凭证无效或已过期").setCode(400);
        }

        String username = dto.getUsername();
        String encryptedPassword = dto.getPassword();

        if (ValidationHelper.validateUsername(username)) {
            log.warn("登录失败：用户名格式错误，username={}", username);
            return SaResult.error("用户名格式错误").setCode(400);
        }

        String rawPassword;
        try {
            rawPassword = RsaUtils.decryptByPrivateKey(encryptedPassword, rsaKeyConfig.getPrivateKeyBase64());
        } catch (Exception e) {
            log.warn("登录失败：密码解密异常，username={}", username);
            return SaResult.error("密码格式错误").setCode(400);
        }
        if (!StringUtils.hasText(rawPassword)) {
            log.warn("登录失败：解密后密码为空，username={}", username);
            return SaResult.error("密码格式错误").setCode(400);
        }

        // 查询用户（过滤逻辑删除）
        SysUser user = sysUserMapper.selectOne(new QueryWrapper<SysUser>()
                .eq("username", username.trim())
                .eq("is_deleted", 0));
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            log.warn("登录失败：用户名或密码错误，username={}", username);
            return SaResult.error("用户名或密码错误").setCode(400);
        }

        // 禁用用户禁止登录
        if (user.getStatus() == null || user.getStatus() == 0) {
            log.warn("登录失败：账号已被禁用，userId={}, username={}", user.getId(), username);
            return SaResult.error("账号已被禁用").setCode(403);
        }

        // 判断是否启用外部授权模式
        boolean isOauthEnabled = dto.getOauthEnabled() != null && dto.getOauthEnabled();

        Map<String, Object> data = new HashMap<>();

        StpUtil.login(user.getId());
        if (isOauthEnabled) {
            // 外部授权模式：生成一次性授权码，同时返回 token
            String token = StpUtil.getTokenValue();
            String code = oauthCodeService.generateCode(user.getId(), token);
            data.put("code", code);
            data.put("expiresIn", 300); // 5分钟过期
            data.put("token", token);
            log.info("外部授权模式登录成功，生成授权码，userId={}", user.getId());
        } else {
            // 正常登录模式：直接返回 token
            log.info("用户登录成功，userId={}", user.getId());
            data.put("token", StpUtil.getTokenValue());
        }

        return SaResult.data(data);
    }

    @Override
    public SaResult exchangeCodeForToken(String code) {
        if (!StringUtils.hasText(code)) {
            log.warn("授权码换取token失败：授权码为空");
            return SaResult.error("授权码不能为空").setCode(400);
        }

        // 消费授权码并获取之前保存的 token
        String token = oauthCodeService.consumeCodeAndGetToken(code);
        if (token == null) {
            log.warn("授权码换取token失败：授权码无效或已过期，code={}", code);
            return SaResult.error("授权码无效或已过期").setCode(400);
        }

        log.info("授权码换取token成功");

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        return SaResult.data(data);
    }


    @Override
    public SaResult getUserProfile(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return SaResult.error("用户不存在").setCode(400);
        }

        // 转换为UserResponseDTO，自动隐藏密码等敏感字段
        UserResponseDTO userDTO = convertToUserResponseDTO(user);

        return SaResult.data(userDTO);
    }

    /**
     * 将SysUser实体转换为UserResponseDTO
     */
    private UserResponseDTO convertToUserResponseDTO(SysUser user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setEmail(user.getEmail());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setStatus(user.getStatus());
        dto.setCreateTime(user.getCreateTime());
        dto.setUpdateTime(user.getUpdateTime());
        return dto;
    }

    @Override
    public SaResult logout() {
        boolean wasLoggedIn = StpUtil.isLogin(); // 检查登出前的登录状态

        try {
            if (wasLoggedIn) {
                StpUtil.logout(); // 只有在用户已登录时才执行登出

                // 登出成功返回成功信息
                return SaResult.data(Map.of(
                        "message", "登出成功",
                        "logoutTime", System.currentTimeMillis(),
                        "wasLoggedIn", true
                ));
            } else {
                // 用户未登录，返回错误状态
                log.warn("登出请求来自未认证会话（可能 token 无效、过期或未提供）");
                return SaResult.error("用户未登录或会话已过期").setCode(401); // 401表示未授权
            }
        } catch (Exception e) {
            log.error("登出时底层存储异常", e);
            return SaResult.error("登出失败").setCode(500);
        }
    }


    @Override
    public SaResult updatePassword(ChangePasswordDTO dto, Long currentUserId) {
        String encryptedOldPassword = dto.getOld_password();
        String encryptedNewPassword = dto.getNew_password();

        if (!StringUtils.hasText(encryptedOldPassword) || !StringUtils.hasText(encryptedNewPassword)) {
            log.warn("密码修改失败：原密码或新密码为空，userId={}", currentUserId);
            return SaResult.error("原密码或新密码不能为空").setCode(400);
        }

        String rawOldPassword, rawNewPassword;
        try {
            rawOldPassword = RsaUtils.decryptByPrivateKey(encryptedOldPassword, rsaKeyConfig.getPrivateKeyBase64());
            rawNewPassword = RsaUtils.decryptByPrivateKey(encryptedNewPassword, rsaKeyConfig.getPrivateKeyBase64());
            if (!StringUtils.hasText(rawOldPassword) || !StringUtils.hasText(rawNewPassword)) {
                log.warn("密码修改失败：解密后密码为空，userId={}", currentUserId);
                return SaResult.error("密码格式错误").setCode(400);
            }
        } catch (Exception e) {
            log.warn("密码修改失败：密码解密异常，userId={}", currentUserId, e);
            return SaResult.error("密码格式错误").setCode(400);
        }

        if (ValidationHelper.validatePassword(rawNewPassword)) {
            log.warn("密码修改失败：新密码格式不符合要求，userId={}", currentUserId);
            return SaResult.error("新密码格式不符合要求").setCode(400);
        }

        if (rawOldPassword.equals(rawNewPassword)) {
            log.warn("密码修改失败：新密码与原密码相同，userId={}", currentUserId);
            return SaResult.error("新密码不能与原密码相同").setCode(400);
        }

        SysUser user = sysUserMapper.selectById(currentUserId);
        if (user == null) {
            log.warn("密码修改失败：用户不存在，userId={}", currentUserId);
            return SaResult.error("用户不存在").setCode(400);
        }

        if (!passwordEncoder.matches(rawOldPassword, user.getPassword())) {
            log.warn("密码修改失败：原密码错误，userId={}", currentUserId);
            return SaResult.error("原密码错误").setCode(400);
        }

        String encodedNewPassword = passwordEncoder.encode(rawNewPassword);
        user.setPassword(encodedNewPassword);
        long timestamp = System.currentTimeMillis();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
        );
        user.setUpdateTime(localDateTime);

        int rows = sysUserMapper.updateById(user);
        if (rows != 1) {
            log.error("密码修改失败：数据库更新失败，userId={}", currentUserId);
            return SaResult.error("密码修改失败，请重试").setCode(400);
        }

        StpUtil.logout(currentUserId);
        log.info("密码修改成功，用户已登出，userId={}", currentUserId);

        Map<String, Object> data = new HashMap<>();
        data.put("message", "密码修改成功，请重新登录");
        return SaResult.data(data);
    }

    // 默认角色编码
    private static final String DEFAULT_ROLE_CODE = "USER";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult register(RegisterDTO dto) {
        // 检查是否开启了邮箱验证注册
        String useEmailConfig = sysConfigMapper.selectValueByKey(CONFIG_KEY_REG_USE_EMAIL);
        boolean useEmail = "true".equalsIgnoreCase(useEmailConfig);

        if (useEmail) {
            return SaResult.error("该注册方式已停用，请使用邮箱验证码注册").setCode(400);
        }

        return doDirectRegister(dto);
    }

    @Override
    public SaResult requestRegisterCode(RegisterCodeRequestDTO dto) {
        // 1. 验证码校验
        SaResult captchaResult = validateCaptcha(dto.getCaptchaVerification(), dto.getUsername());
        if (captchaResult != null) {
            return captchaResult;
        }

        // 2. 校验临时 Token
        String tokenValue = tempLoginTokenService.consumeToken(dto.getTempToken());
        if (!"unbound".equals(tokenValue)) {
            log.warn("请求注册验证码失败：临时 Token 无效或已过期");
            return SaResult.error("临时登录凭证无效或已过期").setCode(400);
        }

        String username = dto.getUsername().trim();
        String email = dto.getEmail().trim();

        // 3. 验证用户名和邮箱格式
        if (ValidationHelper.validateUsername(username)) {
            return SaResult.error("用户名格式错误").setCode(400);
        }
        if (ValidationHelper.validateEmail(email)) {
            return SaResult.error("邮箱格式不正确").setCode(400);
        }

        // 4. 检查用户名、邮箱是否已存在（正式用户表）
        if (sysUserMapper.selectOne(new QueryWrapper<SysUser>()
                .eq("username", username)
                .eq("is_deleted", 0)) != null) {
            return SaResult.error("用户名已存在").setCode(400);
        }
        if (sysUserMapper.selectOne(new QueryWrapper<SysUser>()
                .eq("email", email)
                .eq("is_deleted", 0)) != null) {
            return SaResult.error("邮箱已被注册").setCode(400);
        }

        // 5. 生成6位验证码
        String code = generateRegisterCode();

        // 5.5 解密密码
        String rawPassword;
        try {
            rawPassword = RsaUtils.decryptByPrivateKey(dto.getPassword(), rsaKeyConfig.getPrivateKeyBase64());
        } catch (Exception e) {
            log.warn("请求注册验证码失败：密码解密异常，username={}", username);
            return SaResult.error("密码格式错误").setCode(400);
        }
        if (!StringUtils.hasText(rawPassword)) {
            return SaResult.error("密码格式错误").setCode(400);
        }
        if (ValidationHelper.validatePassword(rawPassword)) {
            return SaResult.error("密码格式不符合要求").setCode(400);
        }

        // 6. 保存待注册用户信息到内存
        try {
            // 保存到内存（会自动覆盖旧记录）
            registerPendingUserService.savePendingUser(email, username, passwordEncoder.encode(rawPassword), code, REGISTER_CODE_EXPIRE_MINUTES);
            log.info("保存待注册用户信息成功，username={}, email={}", username, email);
        } catch (Exception e) {
            log.error("保存待注册用户信息失败，username={}, error={}", username, e.getMessage());
            return SaResult.error("系统异常，请重试").setCode(500);
        }

        // 7. 发送验证码邮件
        SaResult mailResult = mailService.sendRegisterVerificationCode(email, code, username);
        if (mailResult.getCode() != 200) {
            // 发送失败，删除待注册记录
            registerPendingUserService.deletePendingUser(email);
            return mailResult;
        }

        log.info("注册验证码发送成功，username={}, email={}", username, email);
        Map<String, Object> data = new HashMap<>();
        data.put("message", "验证码已发送到您的邮箱，请查收");
        data.put("email", maskEmail(email));
        data.put("expiresIn", REGISTER_CODE_EXPIRE_MINUTES * 60);
        return SaResult.data(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult confirmRegister(RegisterConfirmDTO dto) {
        String email = dto.getEmail().trim();
        String code = dto.getCode().trim();

        if (!StringUtils.hasText(email)) {
            return SaResult.error("邮箱不能为空").setCode(400);
        }
        if (!StringUtils.hasText(code)) {
            return SaResult.error("验证码不能为空").setCode(400);
        }

        // 1. 查询待注册记录
        RegisterPendingUserService.PendingUser pendingUser = registerPendingUserService.getPendingUser(email);

        if (pendingUser == null) {
            log.warn("注册确认失败：未找到待注册记录，email={}", email);
            return SaResult.error("验证码无效，请重新获取").setCode(400);
        }

        // 2. 检查验证码是否过期
        if (LocalDateTime.now().isAfter(pendingUser.getCodeExpireTime())) {
            log.warn("注册确认失败：验证码已过期，email={}", email);
            registerPendingUserService.deletePendingUser(email);
            return SaResult.error("验证码已过期，请重新获取").setCode(400);
        }

        // 3. 验证验证码是否正确
        if (!code.equals(pendingUser.getCode())) {
            log.warn("注册确认失败：验证码错误，email={}, inputCode={}", email, code);
            return SaResult.error("验证码错误").setCode(400);
        }

        // 4. 获取默认注册角色
        String configRoleCode = sysConfigMapper.selectValueByKey(CONFIG_KEY_REGISTER_DEFAULT_ROLE);
        SysRole defaultRole = null;

        if (StringUtils.hasText(configRoleCode)) {
            defaultRole = sysRoleMapper.selectOne(
                    new QueryWrapper<SysRole>()
                            .eq("code", configRoleCode)
                            .eq("is_deleted", 0)
                            .eq("status", 1));
        }

        if (defaultRole == null) {
            defaultRole = sysRoleMapper.selectOne(
                    new QueryWrapper<SysRole>()
                            .eq("code", DEFAULT_ROLE_CODE)
                            .eq("is_deleted", 0)
                            .eq("status", 1));
        }

        if (defaultRole == null) {
            log.error("无可用的注册角色");
            return SaResult.error("系统配置异常，暂无法注册").setCode(500);
        }

        // 5. 创建正式用户
        SysUser user = new SysUser();
        user.setUsername(pendingUser.getUsername());
        user.setNickname(pendingUser.getUsername());
        user.setEmail(pendingUser.getEmail());
        user.setPassword(pendingUser.getPassword());
        user.setStatus(1);
        sysUserMapper.insert(user);

        // 6. 分配默认角色
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(defaultRole.getId());
        sysUserRoleMapper.insert(userRole);

        // 7. 删除待注册记录
        registerPendingUserService.deletePendingUser(email);

        log.info("邮箱验证码注册成功，userId={}, email={}", user.getId(), email);
        Map<String, Object> data = new HashMap<>();
        data.put("message", "注册成功，请登录");
        data.put("userId", user.getId());
        return SaResult.data(data);
    }

    /**
     * 执行直接注册（reg.use-email = false 时）
     */
    private SaResult doDirectRegister(RegisterDTO dto) {
        // 1. 验证码校验
        SaResult captchaResult = validateCaptcha(dto.getCaptchaVerification(), dto.getUsername());
        if (captchaResult != null) {
            return captchaResult;
        }

        // 2. 校验临时 Token
        String tokenValue = tempLoginTokenService.consumeToken(dto.getTempToken());
        if (!"unbound".equals(tokenValue)) {
            log.warn("注册失败：临时 Token 无效或已过期");
            return SaResult.error("临时登录凭证无效或已过期").setCode(400);
        }

        String username = dto.getUsername().trim();
        String email = dto.getEmail().trim();

        if (ValidationHelper.validateUsername(username)) {
            return SaResult.error("用户名格式错误").setCode(400);
        }
        if (ValidationHelper.validateEmail(email)) {
            return SaResult.error("邮箱格式不正确").setCode(400);
        }

        // 3. 解密密码
        String rawPassword;
        try {
            rawPassword = RsaUtils.decryptByPrivateKey(dto.getPassword(), rsaKeyConfig.getPrivateKeyBase64());
        } catch (Exception e) {
            log.warn("注册失败：密码解密异常，username={}", username);
            return SaResult.error("密码格式错误").setCode(400);
        }
        if (!StringUtils.hasText(rawPassword)) {
            return SaResult.error("密码格式错误").setCode(400);
        }
        if (ValidationHelper.validatePassword(rawPassword)) {
            return SaResult.error("密码格式不符合要求").setCode(400);
        }

        // 4. 检查用户名、邮箱是否已存在
        if (sysUserMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username)) != null) {
            return SaResult.error("用户名已存在").setCode(400);
        }
        if (sysUserMapper.selectOne(new QueryWrapper<SysUser>().eq("email", email)) != null) {
            return SaResult.error("邮箱已被注册").setCode(400);
        }

        // 5. 获取默认注册角色
        String configRoleCode = sysConfigMapper.selectValueByKey(CONFIG_KEY_REGISTER_DEFAULT_ROLE);
        SysRole defaultRole = null;

        if (StringUtils.hasText(configRoleCode)) {
            defaultRole = sysRoleMapper.selectOne(
                    new QueryWrapper<SysRole>()
                            .eq("code", configRoleCode)
                            .eq("is_deleted", 0)
                            .eq("status", 1));
            if (defaultRole != null) {
                log.info("使用配置的角色注册，roleCode={}", configRoleCode);
            } else {
                log.warn("配置的角色无效，roleCode={}，尝试使用默认USER角色", configRoleCode);
            }
        } else {
            log.warn("系统配置 user_register_default_role 未设置，尝试使用默认USER角色");
        }

        if (defaultRole == null) {
            defaultRole = sysRoleMapper.selectOne(
                    new QueryWrapper<SysRole>()
                            .eq("code", DEFAULT_ROLE_CODE)
                            .eq("is_deleted", 0)
                            .eq("status", 1));
            if (defaultRole != null) {
                log.info("使用默认USER角色注册");
            }
        }

        if (defaultRole == null) {
            log.error("无可用的注册角色，配置角色={}，默认角色USER也不可用", configRoleCode);
            return SaResult.error("系统配置异常，暂无法注册").setCode(500);
        }

        // 6. 创建用户
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setNickname(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setStatus(1);
        sysUserMapper.insert(user);

        // 7. 分配默认角色
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(defaultRole.getId());
        sysUserRoleMapper.insert(userRole);

        log.info("用户注册成功，userId={}, roleId={}", user.getId(), defaultRole.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("message", "注册成功，请登录");
        data.put("userId", user.getId());
        return SaResult.data(data);
    }

    /**
     * 生成6位数字注册验证码
     */
    private String generateRegisterCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < REGISTER_CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 掩码邮箱，用于显示
     */
    private String maskEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (localPart.length() <= 2) {
            return email;
        }
        return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + domain;
    }

    /**
     * 验证码校验通用方法（基于 TianAi-Captcha 二次验证）。
     *
     * @param captchaVerification 前端传入的验证码校验 ID（来自行为验证码校验成功后的返回值）
     * @param username            用户名（用于日志记录）
     * @return 如果验证失败返回错误结果，验证成功返回 null
     */
    private SaResult validateCaptcha(String captchaVerification, String username) {
        if (!StringUtils.hasText(captchaVerification)) {
            log.warn("验证码校验失败：验证码为空，username={}", username);
            return SaResult.error("验证码已失效，请重新获取").setCode(400);
        }

        if (!(imageCaptchaApplication instanceof SecondaryVerificationApplication secondary)) {
            log.error("验证码校验失败：未开启行为验证码二次验证功能");
            return SaResult.error("验证码服务异常，请稍后重试").setCode(500);
        }

        try {
            boolean success = secondary.secondaryVerification(captchaVerification);
            if (!success) {
                log.warn("验证码校验未通过，username={}", username);
                return SaResult.error("验证码验证失败，请重新获取").setCode(400);
            }
            return null;
        } catch (Exception e) {
            log.warn("验证码校验异常，username={}, ex={}", username, e.getMessage());
            return SaResult.error("验证码校验异常，请重试").setCode(400);
        }
    }

    @Override
    public SaResult updateNickname(UpdateNicknameDTO dto, Long currentUserId) {
        String nickname = dto.getNickname().trim();
        if (!StringUtils.hasText(nickname)) {
            log.warn("昵称修改失败：昵称为空，userId={}", currentUserId);
            return SaResult.error("昵称不能为空").setCode(400);
        }

        if (nickname.length() > 50) {
            log.warn("昵称修改失败：昵称长度超过限制，userId={}", currentUserId);
            return SaResult.error("昵称长度不能超过50字符").setCode(400);
        }

        SysUser user = sysUserMapper.selectById(currentUserId);
        if (user == null) {
            log.warn("昵称修改失败：用户不存在，userId={}", currentUserId);
            return SaResult.error("用户不存在").setCode(400);
        }

        user.setNickname(nickname);
        long timestamp = System.currentTimeMillis();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
        );
        user.setUpdateTime(localDateTime);

        int rows = sysUserMapper.updateById(user);
        if (rows != 1) {
            log.error("昵称修改失败：数据库更新失败，userId={}", currentUserId);
            return SaResult.error("昵称修改失败，请重试").setCode(400);
        }

        log.info("昵称修改成功，userId={}", currentUserId);
        Map<String, Object> data = new HashMap<>();
        data.put("message", "昵称修改成功");
        return SaResult.data(data);
    }

    @Override
    public SaResult updateAvatarUrl(UpdateAvatarUrlDTO dto, Long currentUserId) {
        SysUser user = sysUserMapper.selectById(currentUserId);
        if (user == null) {
            log.warn("头像URL修改失败：用户不存在，userId={}", currentUserId);
            return SaResult.error("用户不存在").setCode(400);
        }

        String avatarUrl = null;
        if (dto.getAvatarUrl() != null) {
            String v = dto.getAvatarUrl().trim();
            if (!v.isEmpty()) {
                if (!isValidAvatarUrl(v)) {
                    log.warn("头像URL修改失败：URL格式无效，userId={}", currentUserId);
                    return SaResult.error("头像URL格式无效，请输入有效的 http/https 链接或传空字符串清空").setCode(400);
                }
                avatarUrl = v;
            }
            // v为空时，avatarUrl保持null，表示清空头像
        }

        // 使用 LambdaUpdateWrapper 来更新，可以正确处理 null 值
        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, currentUserId)
                .set(SysUser::getAvatarUrl, avatarUrl)
                .set(SysUser::getUpdateTime, LocalDateTime.now());

        int rows = sysUserMapper.update(null, updateWrapper);
        if (rows != 1) {
            log.error("头像URL修改失败：数据库更新失败，userId={}", currentUserId);
            return SaResult.error("头像URL修改失败，请重试").setCode(400);
        }

        log.info("头像URL修改成功，userId={}", currentUserId);
        Map<String, Object> data = new HashMap<>();
        data.put("message", avatarUrl == null ? "头像已清空" : "头像URL修改成功");
        return SaResult.data(data);
    }

    @Override
    public SaResult updateEmail(UpdateEmailDTO dto, Long currentUserId) {
        String email = dto.getEmail().trim();
        if (!StringUtils.hasText(email)) {
            log.warn("邮箱修改失败：邮箱为空，userId={}", currentUserId);
            return SaResult.error("邮箱不能为空").setCode(400);
        }

        if (ValidationHelper.validateEmail(email)) {
            log.warn("邮箱修改失败：邮箱格式不正确，userId={}", currentUserId);
            return SaResult.error("邮箱格式不正确").setCode(400);
        }

        // 检查邮箱是否已被其他用户使用
        SysUser existingUser = sysUserMapper.selectOne(new QueryWrapper<SysUser>()
                .eq("email", email)
                .eq("is_deleted", 0));
        if (existingUser != null && !existingUser.getId().equals(currentUserId)) {
            log.warn("邮箱修改失败：邮箱已被注册，userId={}, email={}", currentUserId, email);
            return SaResult.error("邮箱已被注册").setCode(400);
        }

        SysUser user = sysUserMapper.selectById(currentUserId);
        if (user == null) {
            log.warn("邮箱修改失败：用户不存在，userId={}", currentUserId);
            return SaResult.error("用户不存在").setCode(400);
        }

        user.setEmail(email);
        long timestamp = System.currentTimeMillis();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
        );
        user.setUpdateTime(localDateTime);

        int rows = sysUserMapper.updateById(user);
        if (rows != 1) {
            log.error("邮箱修改失败：数据库更新失败，userId={}", currentUserId);
            return SaResult.error("邮箱修改失败，请重试").setCode(400);
        }

        log.info("邮箱修改成功，userId={}", currentUserId);
        Map<String, Object> data = new HashMap<>();
        data.put("message", "邮箱修改成功");
        return SaResult.data(data);
    }

    @Override
    public SaResult getCurrentUserPermissions(Long currentUserId) {
        // 基于角色 + 权限表，计算当前用户拥有的所有权限编码（父权限自动展开为所有子权限）
        SysUser user = sysUserMapper.selectById(currentUserId);
        if (user == null || user.getIsDeleted() != null && user.getIsDeleted() == 1) {
            return SaResult.error("用户不存在").setCode(404);
        }

        List<SysRole> roles = sysRoleMapper.selectRolesByUserId(currentUserId);
        if (roles.isEmpty()) {
            return SaResult.data(List.of());
        }

        List<SysPermission> allPermissions = sysPermissionMapper.selectList(null);
        List<String> allCodes = allPermissions.stream()
                .map(SysPermission::getCode)
                .filter(StringUtils::hasText)
                .toList();

        Set<String> resultCodes = new LinkedHashSet<>();

        for (SysRole role : roles) {
            // 通过权限组获取权限（动态计算，不再依赖 sys_role_permission 表）
            List<SysPermissionGroup> groups = sysPermissionGroupMapper.selectGroupsByRoleId(role.getId());
            for (SysPermissionGroup group : groups) {
                List<SysPermission> rolePermissions = sysPermissionMapper.selectPermissionsByGroupId(group.getId());
                for (SysPermission permission : rolePermissions) {
                    String code = permission.getCode();
                    if (!StringUtils.hasText(code)) {
                        continue;
                    }
                    if (resultCodes.add(code)) {
                        for (String candidate : allCodes) {
                            if (PermissionOverlapHelper.isParentOf(code, candidate)) {
                                resultCodes.add(candidate);
                            }
                        }
                    }
                }
            }
        }

        return SaResult.data(List.copyOf(resultCodes));
    }

    /**
     * 校验为合法的 http/https URL，用于头像等链接
     */
    private boolean isValidAvatarUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URL u = new URL(url);
            String scheme = u.getProtocol();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (MalformedURLException e) {
            return false;
        }
    }
}