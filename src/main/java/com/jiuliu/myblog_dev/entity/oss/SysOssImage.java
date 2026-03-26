/*
 * [SysOssImage.java]
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

package com.jiuliu.myblog_dev.entity.oss;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OSS 图片映射实体
 *
 * <p>用于存储上传到阿里云 OSS 的图片元信息。</p>
 */
@Data
@TableName("sys_oss_image")
public class SysOssImage {

    /**
     * 图片ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 图片哈希值（MD5），用于唯一标识图片
     */
    private String hash;

    /**
     * 原始文件名（截断至128位）
     */
    @TableField("original_name")
    private String originalName;

    /**
     * OSS 对象名称（文件路径）
     */
    @TableField("object_name")
    private String objectName;

    /**
     * 文件大小（字节）
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 上传用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
