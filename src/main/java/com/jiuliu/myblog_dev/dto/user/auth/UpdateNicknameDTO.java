/*
 * [UpdateNicknameDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/20
 */

package com.jiuliu.myblog_dev.dto.user.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateNicknameDTO {

    @NotBlank(message = "昵称不能为空")
    @Size(min = 1, max = 50, message = "昵称长度必须在1-50之间")
    private String nickname;
}
