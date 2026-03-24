/*
 * [PublicCommentResponseDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/24
 */

package com.jiuliu.myblog_dev.dto.comment;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公共评论列表响应DTO
 * 用于返回文章的所有评论（包括子评论）
 */
@Data
public class PublicCommentResponseDTO {

    private Long id;

    private Long parentId;

    private String username;

    private String email;

    private String avatarUrl;

    private String website;

    private String content;

    private Boolean isAdmin;

    private String deviceInfo;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<PublicCommentResponseDTO> children;
}
