package com.jiuliu.myblog_dev.service.user.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.config.RsaKeyConfig;
import com.jiuliu.myblog_dev.dto.user.auth.ChangePasswordDTO;
import com.jiuliu.myblog_dev.dto.user.auth.LoginDTO;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.utils.rsa.RsaUtils;
import com.jiuliu.myblog_dev.utils.Validation.ValidationHelper;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private RsaKeyConfig rsaKeyConfig;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public SaResult getPublicKey() {
        Map<String, Object> data = new HashMap<>();
        data.put("publicKey", rsaKeyConfig.getPublicKeyBase64());
        return SaResult.data(data);
    }

    @Override
    public SaResult login(LoginDTO dto) {
        String username = dto.getUsername();
        String encryptedPassword = dto.getPassword();

        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (!ValidationHelper.validateUsername(username)) {
            throw new IllegalArgumentException("用户名格式错误");
        }
        if (!StringUtils.hasText(encryptedPassword)) {
            throw new IllegalArgumentException("密码不能为空");
        }

        String rawPassword;
        try {
            rawPassword = RsaUtils.decryptByPrivateKey(encryptedPassword, rsaKeyConfig.getPrivateKeyBase64());
        } catch (Exception e) {
            throw new IllegalArgumentException("密码格式错误");
        }

        SysUser user = sysUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysUser>()
                        .eq("username", username.trim())
        );

        if (user == null) {
            throw new IllegalArgumentException("用户名不存在");
        }

        if (ValidationHelper.validatePassword(rawPassword)) {
            throw new IllegalArgumentException("密码格式错误");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }

        StpUtil.login(user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("token", StpUtil.getTokenValue());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("updateTime", user.getUpdateTime());

        return SaResult.data(data);
    }


    @Override
    public SaResult getUserProfile(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
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
        if (!StpUtil.isLogin()) {
            throw new IllegalArgumentException("用户未登录");
        }

        try {
            StpUtil.logout();
            Map<String, Object> data = new HashMap<>();
            data.put("message", "登出成功");
            data.put("logoutTime", System.currentTimeMillis());
            return SaResult.data(data);
        } catch (Exception e) {
            throw new RuntimeException("登出失败", e);
        }
    }

    @Override
    public SaResult updatePassword(ChangePasswordDTO dto, Long currentUserId) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("当前用户未登录");
        }

        String encryptedOldPassword = dto.getOld_password();
        String encryptedNewPassword = dto.getNew_password();

        if (!StringUtils.hasText(encryptedOldPassword) || !StringUtils.hasText(encryptedNewPassword)) {
            throw new IllegalArgumentException("原密码或新密码不能为空");
        }

        String rawOldPassword, rawNewPassword;
        try {
            rawOldPassword = RsaUtils.decryptByPrivateKey(encryptedOldPassword, rsaKeyConfig.getPrivateKeyBase64());
            rawNewPassword = RsaUtils.decryptByPrivateKey(encryptedNewPassword, rsaKeyConfig.getPrivateKeyBase64());
            if (!StringUtils.hasText(rawOldPassword) || !StringUtils.hasText(rawNewPassword)) {
                throw new IllegalArgumentException("密码格式错误");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("密码格式错误");
        }

        if (ValidationHelper.validatePassword(rawNewPassword)) {
            throw new IllegalArgumentException("新密码格式不符合要求");
        }

        if (rawOldPassword.equals(rawNewPassword)) {
            throw new IllegalArgumentException("新密码不能与原密码相同");
        }

        SysUser user = sysUserMapper.selectById(currentUserId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        if (!passwordEncoder.matches(rawOldPassword, user.getPassword())) {
            throw new IllegalArgumentException("原密码错误");
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
            throw new RuntimeException("密码修改失败，请重试");
        }

        StpUtil.logout(currentUserId);

        Map<String, Object> data = new HashMap<>();
        data.put("message", "密码修改成功，请重新登录");
        return SaResult.data(data);
    }
}