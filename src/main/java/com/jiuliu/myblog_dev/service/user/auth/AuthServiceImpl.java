package com.jiuliu.myblog_dev.service.user.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuliu.myblog_dev.config.RsaKeyConfig;
import com.jiuliu.myblog_dev.dto.user.auth.ChangePasswordDTO;
import com.jiuliu.myblog_dev.dto.user.auth.LoginDTO;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.utils.rsa.RsaUtils;
import com.jiuliu.myblog_dev.utils.Validation.ValidationHelper;
import com.jiuliu.myblog_dev.utils.user.auth.TempLoginTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static cn.dev33.satoken.SaManager.log;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private RsaKeyConfig rsaKeyConfig;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private TempLoginTokenService tempLoginTokenService;

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
        // 校验临时 Token
        String tempToken = dto.getTempToken();
        String tokenValue = tempLoginTokenService.consumeToken(tempToken);

        if (!"unbound".equals(tokenValue)) {
            return SaResult.error("临时登录凭证无效或已过期").setCode(400);
        }


        String username = dto.getUsername();
        String encryptedPassword = dto.getPassword();

        if (!ValidationHelper.validateUsername(username)) {
            return SaResult.error("用户名格式错误").setCode(400);
        }


        String rawPassword;

        rawPassword = RsaUtils.decryptByPrivateKey(encryptedPassword, rsaKeyConfig.getPrivateKeyBase64());
        if (!StringUtils.hasText(rawPassword)) {
            return SaResult.error("密码格式错误").setCode(400);
        }


//        if (!ValidationHelper.validatePassword(rawPassword)) {
//            return SaResult.error("密码格式不符合要求").setCode(400);
//        }


        // 查询用户并统一认证失败提示
        SysUser user = sysUserMapper.selectOne(
                new QueryWrapper<SysUser>().eq("username", username.trim())
        );

        if (user == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            return SaResult.error("用户名或密码错误").setCode(400);
        }

        // 登录成功
        StpUtil.login(user.getId());

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

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("nickname", user.getNickname());
        profile.put("email", user.getEmail());
        profile.put("createTime", user.getCreateTime());
        profile.put("updateTime", user.getUpdateTime());
        profile.put("avatarUrl", user.getAvatarUrl());

        return SaResult.data(profile);
    }

    @Override
    public SaResult logout() {


        try {
            StpUtil.logout();
            Map<String, Object> data = new HashMap<>();
            data.put("message", "登出成功");
            data.put("logoutTime", System.currentTimeMillis());
            return SaResult.data(data);
        } catch (Exception e) {
            return SaResult.error("登出失败").setCode(400);
        }
    }

    @Override
    public SaResult updatePassword(ChangePasswordDTO dto, Long currentUserId) {


        String encryptedOldPassword = dto.getOld_password();
        String encryptedNewPassword = dto.getNew_password();

        if (!StringUtils.hasText(encryptedOldPassword) || !StringUtils.hasText(encryptedNewPassword)) {
            return SaResult.error("原密码或新密码不能为空").setCode(400);
        }

        String rawOldPassword, rawNewPassword;
        try {
            rawOldPassword = RsaUtils.decryptByPrivateKey(encryptedOldPassword, rsaKeyConfig.getPrivateKeyBase64());
            rawNewPassword = RsaUtils.decryptByPrivateKey(encryptedNewPassword, rsaKeyConfig.getPrivateKeyBase64());
            if (!StringUtils.hasText(rawOldPassword) || !StringUtils.hasText(rawNewPassword)) {
                return SaResult.error("密码格式错误").setCode(400);
            }
        } catch (Exception e) {
            return SaResult.error("密码格式错误").setCode(400);
        }

        if (ValidationHelper.validatePassword(rawNewPassword)) {
            return SaResult.error("新密码格式不符合要求").setCode(400);
        }

        if (rawOldPassword.equals(rawNewPassword)) {
            return SaResult.error("新密码不能与原密码相同").setCode(400);
        }

        SysUser user = sysUserMapper.selectById(currentUserId);
        if (user == null) {
            return SaResult.error("用户不存在").setCode(400);
        }

        if (!passwordEncoder.matches(rawOldPassword, user.getPassword())) {
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
            return SaResult.error("密码修改失败，请重试").setCode(400);
        }

        StpUtil.logout(currentUserId);

        Map<String, Object> data = new HashMap<>();
        data.put("message", "密码修改成功，请重新登录");
        return SaResult.data(data);
    }
}