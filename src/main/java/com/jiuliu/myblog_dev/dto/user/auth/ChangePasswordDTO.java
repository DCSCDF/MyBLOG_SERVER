/*
 * [ChangePasswordDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/2 18:33
 */

package com.jiuliu.myblog_dev.dto.user.auth;


import lombok.Data;

@Data
public class ChangePasswordDTO {
    private String new_password; //new password
    private String old_password;
}
