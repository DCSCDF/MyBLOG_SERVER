/*
 * [PageUserDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.dto.user.manage;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户分页查询 DTO
 */
@Data
public class PageUserDTO {

    @NotNull(message = "当前页码不能为空")
    @Min(value = 1, message = "当前页码必须大于0")
    private Integer currentPage;

    @NotNull(message = "每页数量不能为空")
    @Min(value = 1, message = "每页数量必须大于0")
    private Integer pageSize;

    /**
     * 搜索关键词（可选）：匹配 username / nickname / email
     */
    private String keyword;

    /**
     * 状态筛选（可选）：0=禁用，1=启用
     */
    private Integer status;
}

