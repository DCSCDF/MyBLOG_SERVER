/*
 * [UpdateAvatarUrlDTO.java]
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

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAvatarUrlDTO {

    @Size(max = 200, message = "头像URL长度不能超过200字符")
    private String avatarUrl; // 可以为空，传空字符串表示清空头像
}
