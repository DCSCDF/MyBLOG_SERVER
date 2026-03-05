/*
 * [SysBlog.java]
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

package com.jiuliu.myblog_dev.entity.blog;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_blog")
public class SysBlog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("category_id")
    private Long categoryId;

    private String title;           // 标题
    private String summary;         // 摘要

    @TableField("content_md")
    private String content;         // MD内容（LONGTEXT）

    @TableField("content_html")
    private String htmlContent;     // HTML内容

    @TableField("cover_image")
    private String coverImage;      // 封面图

    private String tags;            // 标签（逗号分隔）

    @TableField("author_id")
    private Long authorId;

    @TableField("view_count")
    private Integer viewCount = 0;

    @TableField("comment_count")
    private Integer commentCount = 0;

    @TableField("like_count")
    private Integer likeCount = 0;

    @TableField("is_hidden")
    private Boolean hidden;         // 是否私密

    @TableField("is_top")
    private Boolean top;            // 是否置顶

    @TableField("is_recommend")
    private Boolean recommend;      // 是否推荐

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;       // 逻辑删除

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}