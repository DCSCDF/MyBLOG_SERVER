/*
 * [UserInfo.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/8
 */

package com.jiuliu.myblog_dev.dto.blog.global;

import lombok.Data;

/**
 * 用户信息 DTO，包含昵称和状态
 */
@Data
public class UserInfo {
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 用户状态
     */
    private Integer status;
    
    /**
     * 是否删除
     */
    private Integer isDeleted;

    public UserInfo(String nickname, Integer status, Integer isDeleted) {
        this.nickname = nickname;
        this.status = status;
        this.isDeleted = isDeleted;
    }
}
