/*
 * [UserUpdateStatusDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/18 04:41
 */

/*
 * [UserUpdateStatusDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.dto.user.manage;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserUpdateStatusDTO {

    /**
     * 状态：0=禁用，1=启用
     */
    @NotNull(message = "status不能为空")
    private Integer status;
}

