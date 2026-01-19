package com.jiuliu.myblog_dev.service.user.auth;


import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.user.auth.ChangePasswordDTO;
import com.jiuliu.myblog_dev.dto.user.auth.LoginDTO;


public interface AuthService {

    SaResult getPublicKey();
    SaResult login(LoginDTO dto);
    SaResult getUserProfile(Long userId);
    SaResult logout();
    SaResult updatePassword(ChangePasswordDTO dto, Long currentUserId);
//    SaResult getPublicKey();
//    SaResult login(LoginDTO dto);
//    SaResult getUserProfile(Long userId);      // 仍接收 userId（由 Controller 传入）
//    SaResult logout();
//    SaResult updatePassword(ChangePasswordDTO dto, Long currentUserId);
    // TODO: 后续补充 register 方法
}