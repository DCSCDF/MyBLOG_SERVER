/*
 * [FindPasswordConfirmDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/4/12
 */

package com.jiuliu.myblog_dev.dto.user.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FindPasswordConfirmDTO {

    @NotBlank(message = "用户名或邮箱不能为空")
    private String usernameOrEmail;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
