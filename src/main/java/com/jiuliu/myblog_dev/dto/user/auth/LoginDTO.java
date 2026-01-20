package com.jiuliu.myblog_dev.dto.user.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class LoginDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "临时登录凭证不能为空")
    private String tempToken;

    @NotNull(message = "记住我选项不能为空")
    private Boolean rememberMe; // 注意：Boolean 包装类 + 小驼峰命名

    @NotBlank(message = "验证码校验不能为空")
    private String captchaVerification;
}