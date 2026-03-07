/*
 * [SysComment.java]
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

package com.jiuliu.myblog_dev.entity.blog.comment;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_comment") //评论表
public class SysComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("blog_id")
    private Long blogId;            // 所属文章

    @TableField("parent_id")
    private Long parentId = 0L;     // 父评论ID，0=顶级

    @TableField("user_id")
    private Long userId;            // 已登录用户ID（可为空）

    private String username;        // 评论者名称（必填）

    @TableField("email")
    private String email;

    @TableField("avatar_url")
    private String avatarUrl;
    private String website;
    private String content;         // 评论内容

    private Byte status = 0;        // 0=待审,1=通过,2=垃圾,3=删除

    @TableField("like_count")
    private Integer likeCount = 0;

    @TableField("device_info")
    private String deviceInfo;
    private String ipAddress;

    @TableField("is_admin")
    private Boolean admin = false;  // 是否管理员评论

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;       // 逻辑删除：0=未删除，1=已删除

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}