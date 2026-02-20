/*
 * [UserAdminResponseDTO.java]
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
 * [UserAdminResponseDTO.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.dto.user.manage;

import com.jiuliu.myblog_dev.dto.user.role.RoleResponseDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户管理响应 DTO（不包含密码等敏感信息）
 */
@Data
public class UserAdminResponseDTO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatarUrl;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 用户当前角色（列表接口与详情均返回）
     */
    private List<RoleResponseDTO> roles;
}

