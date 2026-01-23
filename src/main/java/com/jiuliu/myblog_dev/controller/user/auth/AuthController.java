package com.jiuliu.myblog_dev.controller.user.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.user.auth.ChangePasswordDTO;
import com.jiuliu.myblog_dev.dto.user.auth.LoginDTO;
import com.jiuliu.myblog_dev.service.user.auth.AuthService;
import com.jiuliu.myblog_dev.utils.rateLimit.RateLimit;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    // 构造函数注入
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/public-key")
//    @RateLimit(count = 6, period = 15)
    public SaResult getPublicKey() {
        return authService.getPublicKey();
    }

    @PostMapping("/login")
    @RateLimit(count = 6, period = 15)
    public SaResult login(@Valid @RequestBody LoginDTO dto) {
        return authService.login(dto);
    }

    @PostMapping("/profile")
    @RateLimit(count = 80, period = 4)
    public SaResult getUserProfile() {
        // 根据 token 返回userID 如果 token 无效会抛 NotLoginException
        Long userId = StpUtil.getLoginIdAsLong(); // 自动由 Sa-Token 提供
        return authService.getUserProfile(userId);
    }

    @PostMapping("/logout")
    @RateLimit(count = 80, period = 4)
    public SaResult logout() {
        return authService.logout();
    }

    @PostMapping("/update-password")
    @RateLimit(count = 1, period = 15)
    public SaResult updatePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return authService.updatePassword(dto, currentUserId);
    }

//    @PostMapping("/register")
//    public SaResult register() {
//        // TODO: 补充注册逻辑
//        return SaResult.error("功能暂未开放").setCode(501);
//    }
}