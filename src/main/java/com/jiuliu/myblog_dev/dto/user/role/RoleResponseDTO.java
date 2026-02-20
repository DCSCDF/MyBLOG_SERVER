/*
 * [RoleResponseDTO.java]
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
 * [RoleResponseDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.dto.user.role;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoleResponseDTO {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean superAdmin;
    private Boolean isSystem;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
