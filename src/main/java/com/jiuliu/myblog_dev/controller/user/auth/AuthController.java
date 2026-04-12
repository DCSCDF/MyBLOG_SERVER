/*
 * [AuthController.java]
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

package com.jiuliu.myblog_dev.controller.user.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.user.auth.*;
import com.jiuliu.myblog_dev.service.user.auth.AuthService;
import com.jiuliu.myblog_dev.utils.rateLimit.RateLimit;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
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

    /**
     * 处理返回Map类型的SaResult结果
     *
     * @param saResult Sa-Token返回的结果
     * @return 统一响应格式
     */
    private Response<Map<String, Object>> handleSaResult(SaResult saResult) {
        if (saResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) saResult.getData();
            return ResponseUtil.success(data, 200);
        } else {
            return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
        }
    }

    /**
     * 处理返回Object类型的SaResult结果
     *
     * @param saResult Sa-Token返回的结果
     * @return 统一响应格式
     */
    private Response<Object> handleSaResultObject(SaResult saResult) {
        if (saResult.getCode() == 200) {
            return ResponseUtil.success(saResult.getData(), 200);
        } else {
            return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
        }
    }

    /**
     * 获取登录用公钥（限流：每 IP 每 1 分钟最多 30 次，防止滥用）
     */
    @GetMapping("/public-key")
    @RateLimit(count = 30, period = 1)
    public Response<Map<String, Object>> getPublicKey() {
        return handleSaResult(authService.getPublicKey());
    }

    @PostMapping("/login")
    @RateLimit(count = 6, period = 15)
    public Response<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        return handleSaResult(authService.login(dto));
    }

    /**
     * OAuth 授权码换取 token
     * 用于外部授权模式，code 只能使用一次，有效期5分钟
     */
    @PostMapping("/oauth/token")
    @RateLimit(count = 10, period = 60)
    public Response<Map<String, Object>> exchangeCodeForToken(@Valid @RequestBody OAuthCodeDTO dto) {
        return handleSaResult(authService.exchangeCodeForToken(dto.getCode()));
    }

    @PostMapping("/profile")
    @RateLimit(count = 80, period = 4)
    public Response<Object> getUserProfile() {
        // 根据 token 返回userID 如果 token 无效会抛 NotLoginException
        Long userId = StpUtil.getLoginIdAsLong(); // 自动由 Sa-Token 提供
        return handleSaResultObject(authService.getUserProfile(userId));
    }

    @PostMapping("/logout")
    @RateLimit(count = 80, period = 4)
    public Response<Map<String, Object>> logout() {
        return handleSaResult(authService.logout());
    }

    @PostMapping("/update-password")
    @RateLimit(count = 6, period = 15)
    public Response<Map<String, Object>> updatePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return handleSaResult(authService.updatePassword(dto, currentUserId));
    }

    @PostMapping("/register")
    @RateLimit(count = 6, period = 60)
    public Response<Map<String, Object>> register(@Valid @RequestBody RegisterDTO dto) {
        return handleSaResult(authService.register(dto));
    }

    /**
     * 请求发送注册验证码（当 reg.use-email 为 true 时使用）
     * 该接口会验证用户信息，发送验证码到邮箱，并返回验证码有效期等信息
     */
    @PostMapping("/register/code")
    @RateLimit(count = 6, period = 60)
    public Response<Map<String, Object>> requestRegisterCode(@Valid @RequestBody RegisterCodeRequestDTO dto) {
        return handleSaResult(authService.requestRegisterCode(dto));
    }

    /**
     * 确认注册（验证邮箱验证码并完成注册）
     * 用户需要提供邮箱和收到的验证码来正式完成注册
     */
    @PostMapping("/register/confirm")
    @RateLimit(count = 6, period = 60)
    public Response<Map<String, Object>> confirmRegister(@Valid @RequestBody RegisterConfirmDTO dto) {
        return handleSaResult(authService.confirmRegister(dto));
    }

    @PostMapping("/update-nickname")
    @RateLimit(count = 10, period = 60)
    public Response<Map<String, Object>> updateNickname(@Valid @RequestBody UpdateNicknameDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return handleSaResult(authService.updateNickname(dto, currentUserId));
    }

    @PostMapping("/update-avatar-url")
    @RateLimit(count = 10, period = 60)
    public Response<Map<String, Object>> updateAvatarUrl(@Valid @RequestBody UpdateAvatarUrlDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return handleSaResult(authService.updateAvatarUrl(dto, currentUserId));
    }

    /**
     * 直接修改邮箱（当 reg.use-email 为 false 时使用）
     * 直接传入新邮箱进行修改，无需验证码
     */
    @PostMapping("/update-email")
    @RateLimit(count = 6, period = 60)
    public Response<Map<String, Object>> updateEmail(@Valid @RequestBody UpdateEmailDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return handleSaResult(authService.updateEmail(dto, currentUserId));
    }

    /**
     * 请求发送邮箱变更验证码（当 reg.use-email 为 true 时使用）
     */
    @PostMapping("/change-email/code")
    @RateLimit(count = 6, period = 60)
    public Response<Map<String, Object>> requestChangeEmailCode(@Valid @RequestBody ChangeEmailDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return handleSaResult(authService.requestChangeEmailCode(dto, currentUserId));
    }

    /**
     * 确认邮箱变更（验证邮箱验证码并完成邮箱更换）
     */
    @PostMapping("/change-email/confirm")
    @RateLimit(count = 6, period = 60)
    public Response<Map<String, Object>> confirmChangeEmail(@Valid @RequestBody ChangeEmailConfirmDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return handleSaResult(authService.confirmChangeEmail(dto, currentUserId));
    }

    /**
     * 获取当前登录用户拥有的权限编码列表
     * 包含父权限自动展开得到的所有子权限
     */
    @PostMapping("/permissions")
    @RateLimit(count = 80, period = 4)
    public Response<Object> getCurrentUserPermissions() {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return handleSaResultObject(authService.getCurrentUserPermissions(currentUserId));
    }
}