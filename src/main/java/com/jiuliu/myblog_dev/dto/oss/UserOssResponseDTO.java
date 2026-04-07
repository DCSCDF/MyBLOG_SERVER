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
 * UpdateTime: 2026/4/4
 */

package com.jiuliu.myblog_dev.dto.oss;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户OSS响应DTO
 *
 * <p>包含图片的基本信息和多尺寸访问URL。</p>
 *
 * <p><b>图片尺寸说明：</b></p>
 * <table border="1">
 *   <tr><th>字段</th><th>尺寸</th><th>用途</th></tr>
 *   <tr><td>thumbnailUrl</td><td>200x200</td><td>列表缩略图</td></tr>
 *   <tr><td>smallUrl</td><td>400x400</td><td>小图展示</td></tr>
 *   <tr><td>mediumUrl</td><td>800x800</td><td>中等尺寸</td></tr>
 *   <tr><td>largeUrl</td><td>1200x1200</td><td>大图展示</td></tr>
 *   <tr><td>url</td><td>原图</td><td>原图下载/预览</td></tr>
 * </table>
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

    /**
     * 缩略图 URL (200x200)
     */
    private String thumbnailUrl;

    /**
     * 小图 URL (400x400)
     */
    private String smallUrl;

    /**
     * 中图 URL (800x800)
     */
    private String mediumUrl;

    /**
     * 大图 URL (1200x1200)
     */
    private String largeUrl;

    /**
     * 原图 URL
     */
    private String url;
}
