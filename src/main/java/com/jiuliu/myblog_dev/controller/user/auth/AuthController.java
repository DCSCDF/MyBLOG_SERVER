/*
 * [AuthController.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/1/31 14:53
 */

package com.jiuliu.myblog_dev.controller.user.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.user.auth.ChangePasswordDTO;
import com.jiuliu.myblog_dev.dto.user.auth.LoginDTO;
import com.jiuliu.myblog_dev.service.user.auth.AuthService;
import com.jiuliu.myblog_dev.utils.ResponseUtil;
import com.jiuliu.myblog_dev.utils.rateLimit.RateLimit;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


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
    public Response<Map<String, Object>> getPublicKey() {
        SaResult saResult = authService.getPublicKey();
        if (saResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) saResult.getData();
            return ResponseUtil.success(data, 200);
        } else {
            return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
        }
    }

    @PostMapping("/login")
    @RateLimit(count = 6, period = 15)
    public Response<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        SaResult saResult = authService.login(dto);
        if (saResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) saResult.getData();
            return ResponseUtil.success(data, 200);
        } else {
            return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
        }
    }

    @PostMapping("/profile")
    @RateLimit(count = 80, period = 4)
    public Response<Object> getUserProfile() {
        // 根据 token 返回userID 如果 token 无效会抛 NotLoginException
        Long userId = StpUtil.getLoginIdAsLong(); // 自动由 Sa-Token 提供
        SaResult saResult = authService.getUserProfile(userId);
        if (saResult.getCode() == 200) {
            return ResponseUtil.success(saResult.getData(), 200);
        } else {
            return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
        }
    }

    @PostMapping("/logout")
    @RateLimit(count = 80, period = 4)
    public Response<Map<String, Object>> logout() {
        SaResult saResult = authService.logout();
        if (saResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) saResult.getData();
            return ResponseUtil.success(data, 200);
        } else {
            return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
        }
    }

    @PostMapping("/update-password")
    @RateLimit(count = 1, period = 15)
    public Response<Map<String, Object>> updatePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        SaResult saResult = authService.updatePassword(dto, currentUserId);
        if (saResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) saResult.getData();
            return ResponseUtil.success(data, 200);
        } else {
            return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
        }
    }

    @PostMapping("/register")
    public Response<Void> register() {
        // TODO: 补充注册逻辑
        return ResponseUtil.fail("功能暂未开放", 501); // 501表示未实现
    }
}