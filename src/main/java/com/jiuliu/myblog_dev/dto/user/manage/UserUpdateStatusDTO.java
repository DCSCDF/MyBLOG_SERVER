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

