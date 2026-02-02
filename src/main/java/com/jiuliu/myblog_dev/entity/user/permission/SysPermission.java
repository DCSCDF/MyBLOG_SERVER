/*
 * [SysPermission.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/1/13 09:06
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

    @TableField("parent_id")
    private Long parentId;           // 父权限ID，0表示顶级

    private String code;             // 权限编码（唯一）
    private String name;             // 权限名称
    private String type;             // 类型：MENU/BUTTON/API/FIELD
    private String description;      // 描述
    private String icon;             // 图标
    private String path;             // 路径/URL
    private String component;        // 前端组件

    @TableField("is_hidden")
    private Boolean hidden;          // 是否隐藏

    @TableField("is_affix")
    private Boolean affix;           // 是否固定页签

    @TableField("is_keep_alive")
    private Boolean keepAlive;       // 是否缓存

    @TableField("sort_order")
    private Integer sortOrder;
    private Integer status;          // 0=禁用，1=启用

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}