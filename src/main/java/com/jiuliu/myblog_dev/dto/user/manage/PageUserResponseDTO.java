/*
 * [PageUserResponseDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.dto.user.manage;

import com.jiuliu.myblog_dev.dto.common.FilterOptionItem;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PageUserResponseDTO {

    private List<UserAdminResponseDTO> records;
    private Long total;
    private Long size;
    private Long current;
    private Long pages;
    /** 可用的筛选项（如 status：启用/禁用），供前端渲染筛选控件 */
    private Map<String, List<FilterOptionItem>> filterOptions;
}

