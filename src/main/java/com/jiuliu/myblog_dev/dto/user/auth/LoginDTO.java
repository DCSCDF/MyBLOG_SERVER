/*
 * [LoginDTO.java]
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

package com.jiuliu.myblog_dev.dto.user.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class LoginDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "临时登录凭证不能为空")
    private String tempToken;

//    @NotNull(message = "记住我选项不能为空")
//    private Boolean rememberMe;

    @NotBlank(message = "验证码校验不能为空")
    private String captchaVerification;

    /**
     * 是否启用外部授权模式
     * false: 正常登录，直接返回token
     * true: 返回一次性code，需通过 /api/oauth/token 接口换取token
     */
    private Boolean oauthEnabled;
}