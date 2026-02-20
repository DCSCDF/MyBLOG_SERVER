/*
 * [UserUpdateDTO.java]
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

/*
 * [UserUpdateDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.dto.user.manage;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户信息更新 DTO（管理员）
 * 可修改：昵称、头像URL、角色
 */
@Data
public class UserUpdateDTO {

    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;

    @Size(max = 200, message = "头像URL长度不能超过200")
    private String avatarUrl;

    /**
     * 角色ID（可选）。传入时会覆盖用户现有角色（替换为单一角色）。
     */
    private Long roleId;
}

