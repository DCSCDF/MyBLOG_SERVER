/*
 * [PagePermissionDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/11 16:30
 */

package com.jiuliu.myblog_dev.dto.user.permission;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 权限分页查询 DTO
 */
@Data
public class PagePermissionDTO {

    /**
     * 当前页码（从1开始）
     */
    @NotNull(message = "当前页码不能为空")
    @Min(value = 1, message = "当前页码必须大于0")
    private Integer currentPage;

    /**
     * 每页显示数量
     */
    @NotNull(message = "每页数量不能为空")
    @Min(value = 1, message = "每页数量必须大于0")
    private Integer pageSize;
}