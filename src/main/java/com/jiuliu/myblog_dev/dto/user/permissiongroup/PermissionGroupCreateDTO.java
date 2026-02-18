/*
 * [PermissionGroupCreateDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/18 04:24
 */

/*
 * [PermissionGroupCreateDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.dto.user.permissiongroup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PermissionGroupCreateDTO {

    @NotBlank(message = "权限组名称不能为空")
    @Size(max = 50, message = "权限组名称最大50字符")
    private String name;

    @Size(max = 200, message = "权限组描述最大200字符")
    private String description;

    private Integer sortOrder = 0;
    private Integer status = 1;  // 0=禁用，1=启用
}
