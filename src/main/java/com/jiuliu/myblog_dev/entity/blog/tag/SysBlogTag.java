/*
 * [SysBlogTag.java]
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

package com.jiuliu.myblog_dev.entity.blog.tag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_blog_tag")//文章-标签关联表
public class SysBlogTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("blog_id")
    private Long blogId;

    @TableField("tag_id")
    private Long tagId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}