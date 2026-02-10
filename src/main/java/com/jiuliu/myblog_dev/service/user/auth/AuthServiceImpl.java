/*
 * [AuthServiceImpl.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/1/24 02:43
 */

package com.jiuliu.myblog_dev.service.user.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuliu.myblog_dev.config.RsaKeyConfig;
import com.jiuliu.myblog_dev.dto.user.auth.ChangePasswordDTO;
import com.jiuliu.myblog_dev.dto.user.auth.LoginDTO;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.config.validation.ValidationHelper;
import com.jiuliu.myblog_dev.config.rsa.RsaUtils;
import com.jiuliu.myblog_dev.utils.user.auth.TempLoginTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final SysUserMapper sysUserMapper;
    private final RsaKeyConfig rsaKeyConfig;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TempLoginTokenService tempLoginTokenService;
    private final CaptchaService captchaService;

    // 构造函数注入
    public AuthServiceImpl(
            SysUserMapper sysUserMapper,
            RsaKeyConfig rsaKeyConfig,
            BCryptPasswordEncoder passwordEncoder,
            TempLoginTokenService tempLoginTokenService,
            CaptchaService captchaService) {
        this.sysUserMapper = sysUserMapper;
        this.rsaKeyConfig = rsaKeyConfig;
        this.passwordEncoder = passwordEncoder;
        this.tempLoginTokenService = tempLoginTokenService;
        this.captchaService = captchaService;
    }

//        400: '请求参数错误',
//        401: '未授权，请重新登录',
//        403: '拒绝访问',
//        404: '请求的资源不存在',
//        408: '请求超时',
//        429: '请求过于频繁',
//        500: '服务器内部错误',
//        502: '网关错误',
//        503: '服务不可用',
//        504: '网关超时'


    @Override
    public SaResult getPublicKey() {
        log.info("获取 RSA 公钥及临时 Token");
        Map<String, Object> data = new HashMap<>();
        data.put("publicKey", rsaKeyConfig.getPublicKeyBase64());

        // 生成一个未绑定用户的临时 Token（60秒有效）
        String tempToken = tempLoginTokenService.generateTempToken();
        data.put("tempToken", tempToken);

        log.debug("公钥已返回，临时 Token: {}", tempToken);
        return SaResult.data(data);
    }

    @Override
    public SaResult login(LoginDTO dto) {
        log.info("用户尝试登录，用户名: {}", dto.getUsername());

        // 验证码校验（保持不变）
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaVerification(dto.getCaptchaVerification());
        com.anji.captcha.model.common.ResponseModel response = captchaService.verification(captchaVO);

        if (!response.isSuccess()) {
            String repCode = response.getRepCode();
            String message;
            int httpCode = 400;

            switch (repCode) {
                case "6110":
                    message = "验证码已失效，请重新获取";
                    break;
                case "6111":
                    message = "验证码验证失败";
                    break;
                case "6206":
                    message = "无效验证码请求，请重新获取";
                    break;
                case "6202":
                    message = "验证码验证失败次数过多，请稍后再试";
                    httpCode = 429;
                    break;
                case "6201":
                case "6204":
                    message = "请求过于频繁，请稍后再试";
                    httpCode = 429;
                    break;
                default:
                    message = "验证码校验异常，请重试";
            }

            log.warn("登录失败：验证码校验未通过，repCode={}, username={}", repCode, dto.getUsername());
            return SaResult.error(message).setCode(httpCode);
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

        if (!ValidationHelper.validateUsername(username)) {
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

        // 查询用户
        SysUser user = sysUserMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username.trim()));
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            log.warn("登录失败：用户名或密码错误，username={}", username);
            return SaResult.error("用户名或密码错误").setCode(400);
        }


        StpUtil.login(user.getId()); // 等价于 StpUtil.login(user.getId(), false)

        log.info("用户登录成功，userId={}，", user.getId());

        Map<String, Object> data = new HashMap<>();

        data.put("token", StpUtil.getTokenValue());
        return SaResult.data(data);
    }


    @Override
    public SaResult getUserProfile(Long userId) {
        log.info("获取用户资料，userId={}", userId);

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            log.warn("获取用户资料失败：用户不存在，userId={}", userId);
            return SaResult.error("用户不存在").setCode(400);
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("nickname", user.getNickname());
        profile.put("email", user.getEmail());
        profile.put("createTime", user.getCreateTime());
        profile.put("updateTime", user.getUpdateTime());
        profile.put("avatarUrl", user.getAvatarUrl());

        log.debug("用户资料查询成功，userId={}", userId);
        return SaResult.data(profile);
    }

    @Override
    public SaResult logout() {
        boolean wasLoggedIn = StpUtil.isLogin(); // 检查登出前的登录状态

        try {
            if (wasLoggedIn) {
                StpUtil.logout(); // 只有在用户已登录时才执行登出
                log.info("用户已成功登出");

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
        log.info("用户尝试修改密码，currentUserId={}", currentUserId);

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
}