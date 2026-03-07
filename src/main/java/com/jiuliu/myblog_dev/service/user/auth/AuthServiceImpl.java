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
import com.jiuliu.myblog_dev.config.RsaKeyConfig;
import com.jiuliu.myblog_dev.config.rsa.RsaUtils;
import com.jiuliu.myblog_dev.config.validation.ValidationHelper;
import com.jiuliu.myblog_dev.dto.user.UserResponseDTO;
import com.jiuliu.myblog_dev.dto.user.auth.*;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.entity.user.SysUserRole;
import com.jiuliu.myblog_dev.entity.user.permission.SysPermission;
import com.jiuliu.myblog_dev.entity.user.role.SysRole;
import com.jiuliu.myblog_dev.mapper.config.SysConfigMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserRoleMapper;
import com.jiuliu.myblog_dev.mapper.user.permission.SysPermissionMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRoleMapper;
import com.jiuliu.myblog_dev.utils.user.auth.TempLoginTokenService;
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final String CONFIG_KEY_REGISTER_DEFAULT_ROLE = "user_register_default_role";

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysConfigMapper sysConfigMapper;
    private final RsaKeyConfig rsaKeyConfig;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TempLoginTokenService tempLoginTokenService;
    private final ImageCaptchaApplication imageCaptchaApplication;

    public AuthServiceImpl(
            SysUserMapper sysUserMapper,
            SysUserRoleMapper sysUserRoleMapper,
            SysRoleMapper sysRoleMapper,
            SysPermissionMapper sysPermissionMapper,
            SysConfigMapper sysConfigMapper,
            RsaKeyConfig rsaKeyConfig,
            BCryptPasswordEncoder passwordEncoder,
            TempLoginTokenService tempLoginTokenService,
            ImageCaptchaApplication imageCaptchaApplication) {
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysConfigMapper = sysConfigMapper;
        this.rsaKeyConfig = rsaKeyConfig;
        this.passwordEncoder = passwordEncoder;
        this.tempLoginTokenService = tempLoginTokenService;
        this.imageCaptchaApplication = imageCaptchaApplication;
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

        StpUtil.login(user.getId()); // 等价于 StpUtil.login(user.getId(), false)

        log.info("用户登录成功，userId={}，", user.getId());

        Map<String, Object> data = new HashMap<>();

        data.put("token", StpUtil.getTokenValue());
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
        if (!ValidationHelper.validateEmail(email)) {
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

        // 5. 获取默认注册角色（检查角色存在、未删除、已启用）
        String configRoleCode = sysConfigMapper.selectValueByKey(CONFIG_KEY_REGISTER_DEFAULT_ROLE);
        SysRole defaultRole = null;

        if (StringUtils.hasText(configRoleCode)) {
            // 检查配置的角色是否存在且有效（未删除且启用）
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

        // 如果配置的角色无效，尝试使用默认USER角色
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

        // 如果没有可用的角色，返回错误
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

        if (!ValidationHelper.validateEmail(email)) {
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
            List<SysPermission> rolePermissions = sysPermissionMapper.selectPermissionsByRoleId(role.getId());
            for (SysPermission permission : rolePermissions) {
                String code = permission.getCode();
                if (!StringUtils.hasText(code)) {
                    continue;
                }
                if (resultCodes.add(code)) {
                    for (String candidate : allCodes) {
                        if (com.jiuliu.myblog_dev.utils.PermissionOverlapHelper.isParentOf(code, candidate)) {
                            resultCodes.add(candidate);
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