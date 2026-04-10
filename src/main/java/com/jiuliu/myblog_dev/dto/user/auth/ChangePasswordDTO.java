/*
 * [ChangePasswordDTO.java]
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


import com.jiuliu.myblog_dev.utils.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordDTO {

    @NotBlank(message = "新密码不能为空")
    @ValidPassword
    private String new_password; //new password

    @NotBlank(message = "旧密码不能为空")
    private String old_password;
}
