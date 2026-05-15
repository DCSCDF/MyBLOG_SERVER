/*
 * [SysUser.java]
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

package com.jiuliu.myblog_dev.entity.user;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    /**
     * 用户ID，自增主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名（唯一，非空）
     */
    private String username;

    /**
     * 昵称（非空）
     */
    private String nickname;

    /**
     * 密码（加密后存储，非空）
     */
    private String password;

    /**
     * 邮箱（唯一，可为空）
     */
    private String email;

    /**
     * 头像URL（对应数据库字段 avatar_url）
     */
    @TableField("avatar_url") // 显式指定列名，避免驼峰转下划线不一致风险
    private String avatarUrl;

    /**
     * 状态：0=禁用，1=启用，默认1
     */
    private Integer status;

    /**
     * 逻辑删除：0=未删除，1=已删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 创建时间，默认 CURRENT_TIMESTAMP
     * 注意：MySQL 自动填充，Java 不需手动设值
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间，默认 CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
     * MyBatis-Plus 无法自动处理 ON UPDATE，通常由数据库维护，
     * 但为兼容性，仍标记为 INSERT_UPDATE，配合 MetaObjectHandler 可选处理
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


}

