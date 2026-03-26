/*
 * [UserOssResponseDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/26
 */

package com.jiuliu.myblog_dev.dto.oss;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户OSS响应DTO
 */
@Data
public class UserOssResponseDTO {

    /**
     * 图片ID
     */
    private Long id;

    /**
     * 图片哈希值（MD5）
     */
    private String hash;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * OSS 对象名称（文件路径）
     */
    private String objectName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
