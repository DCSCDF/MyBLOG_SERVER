package com.jiuliu.myblog_dev.controller.user.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.utils.rateLimit.RateLimit;
import com.jiuliu.myblog_dev.dto.user.auth.ChangePasswordDTO;
import com.jiuliu.myblog_dev.dto.user.auth.LoginDTO;
import com.jiuliu.myblog_dev.service.user.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/public-key")
    public SaResult getPublicKey() {
        return authService.getPublicKey();
    }

    @PostMapping("/login")
    public SaResult login(@Valid @RequestBody LoginDTO dto) {
        return authService.login(dto);
    }

    @GetMapping("/profile")
    @RateLimit(count = 40, period = 15)
    public SaResult getUserProfile() {
        Long userId = StpUtil.getLoginIdAsLong(); // 自动由 Sa-Token 提供，若未登录会抛 NotLoginException
        return authService.getUserProfile(userId);
    }

    @PostMapping("/logout")
    @RateLimit(count = 10, period = 15)
    public SaResult logout() {
        return authService.logout();
    }

    @PostMapping("/update-password")
    @RateLimit(count = 1, period = 15)
    public SaResult updatePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return authService.updatePassword(dto, currentUserId);
    }

    @PostMapping("/register")
    public SaResult register() {
        // TODO: 补充注册逻辑
        return SaResult.error("功能暂未开放").setCode(501);
    }
}