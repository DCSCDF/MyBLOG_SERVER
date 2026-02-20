/*
 * [SysPermission.java]
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

package com.jiuliu.myblog_dev.entity.user.permission;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_permission") //权限表
public class SysPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;             // 权限编码（唯一）
    private String name;             // 权限名称
    private String description;      // 描述

    @TableField("sort_order")
    private Integer sortOrder;       // 排序顺序

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}