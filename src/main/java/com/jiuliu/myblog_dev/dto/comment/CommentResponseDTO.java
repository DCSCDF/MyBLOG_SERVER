/*
 * [CommentResponseDTO.java]
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

package com.jiuliu.myblog_dev.dto.comment;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论响应DTO
 */
@Data
public class CommentResponseDTO {

    private Long id;

    /**
     * 关联的文章ID
     */
    private Long blogId;

    /**
     * 关联的文章标题
     */
    private String blogTitle;

    /**
     * 父评论ID，0表示顶级评论
     */
    private Long parentId;

    /**
     * 评论者名称
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 个人网站
     */
    private String website;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 状态：0=待审核，1=已通过，2=垃圾评论
     */
    private Integer status;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 设备信息
     */
    private String deviceInfo;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 是否管理员评论
     */
    private Boolean isAdmin;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
