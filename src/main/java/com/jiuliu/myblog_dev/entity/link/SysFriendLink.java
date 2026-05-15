/*
 * [SysFriendLink.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/23
 */

package com.jiuliu.myblog_dev.entity.link;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_friend_link")
public class SysFriendLink {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 链接名称
     */
    private String name;

    /**
     * URL 地址
     */
    private String url;

    /**
     * 简介
     */
    private String summary;

    /**
     * 备注
     */
    private String remark;

    /**
     * 图片 URL
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 排序顺序（数字越大越靠前）
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 审核状态：0=待审核，1=已通过，2=已拒绝
     */
    private Integer status;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

