/*
 * [PagePermissionGroupResponseDTO.java]
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
 * [PagePermissionGroupResponseDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.dto.user.permissiongroup;

import com.jiuliu.myblog_dev.dto.common.FilterOptionItem;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PagePermissionGroupResponseDTO {

    private List<PermissionGroupResponseDTO> records;
    private Long total;
    private Long size;
    private Long current;
    private Long pages;
    /**
     * 可用的筛选项（如 status、isSystem），供前端渲染筛选控件
     */
    private Map<String, List<FilterOptionItem>> filterOptions;
}
