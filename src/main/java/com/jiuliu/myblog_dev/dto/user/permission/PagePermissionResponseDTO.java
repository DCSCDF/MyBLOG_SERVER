/*
 * [PagePermissionResponseDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/16 17:30
 */

package com.jiuliu.myblog_dev.dto.user.permission;

import com.jiuliu.myblog_dev.dto.common.FilterOptionItem;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 权限分页响应DTO
 * 用于封装权限分页查询结果
 */
@Data
public class PagePermissionResponseDTO {

    private List<PermissionResponseDTO> records;
    private Long total;
    private Long size;
    private Long current;
    private Long pages;
    /** 可用的筛选项（权限无状态/内置等，可为空 Map），供前端统一处理 */
    private Map<String, List<FilterOptionItem>> filterOptions;
}