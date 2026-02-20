/*
 * [RoleCreateDTO.java]
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
 * [RoleCreateDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.dto.user.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleCreateDTO {

    @NotBlank(message = "角色编码不能为空")
    @Size(min = 2, max = 50, message = "角色编码长度必须在2-50之间")
    @Pattern(regexp = "^[A-Z_]+$", message = "角色编码只能包含大写字母和下划线")
    private String code;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称最大50字符")
    private String name;

    @Size(max = 200, message = "角色描述最大200字符")
    private String description;

    private Integer sortOrder = 0;
    private Integer status = 1;  // 0=禁用，1=启用
}
