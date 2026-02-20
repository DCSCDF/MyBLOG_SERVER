/*
 * [SysCategory.java]
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

package com.jiuliu.myblog_dev.entity.blog.category;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_category")//分类
public class SysCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;            // 分类名称
    private String description;     // 描述

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField("is_hidden")
    private Boolean hidden;         // 是否隐藏

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}