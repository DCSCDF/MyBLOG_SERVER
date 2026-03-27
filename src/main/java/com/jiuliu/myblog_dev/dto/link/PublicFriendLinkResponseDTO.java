/*
 * [PublicFriendLinkResponseDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/27
 */

package com.jiuliu.myblog_dev.dto.link;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公共外链响应DTO
 */
@Data
public class PublicFriendLinkResponseDTO {

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
     * 站点图片URL
     */
    private String imageUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
