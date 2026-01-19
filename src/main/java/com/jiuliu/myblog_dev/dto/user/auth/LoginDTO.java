package com.jiuliu.myblog_dev.dto.user.auth;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class LoginDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "临时登录凭证不能为空")
    private String tempToken;
}