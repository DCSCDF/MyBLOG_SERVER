/*
 * [OssService.java]
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

package com.jiuliu.myblog_dev.service.oss;

import cn.dev33.satoken.util.SaResult;

/**
 * OSS 对象存储服务接口
 */
public interface OssService {

    /**
     * 测试 OSS 连接
     *
     * @return SaResult
     */
    SaResult testConnection();

    /**
     * 上传图片
     *
     * <p>支持格式：jpg, jpeg, png, gif, bmp, webp<br>
     * 上传前会进行格式校验和无损压缩。<br>
     * 相同内容的图片会跳过重复上传，直接返回已有记录。</p>
     *
     * @param fileName    原始文件名
     * @param fileBytes   图片字节数据
     * @param contentType MIME 类型
     * @param userId      上传用户 ID
     * @return SaResult，包含图片哈希值和 URL
     */
    SaResult uploadImage(String fileName, byte[] fileBytes, String contentType, Long userId);

    /**
     * 删除图片（通过对象名称）
     *
     * @param objectName OSS 对象名称
     * @param userId     用户 ID（用于日志记录）
     * @return SaResult
     */
    SaResult deleteImage(String objectName, Long userId);

    /**
     * 删除图片（通过哈希值）
     *
     * @param hash   图片哈希值（MD5）
     * @param userId 当前登录用户ID（用于权限校验）
     * @return SaResult
     */
    SaResult deleteImageByHash(String hash, Long userId);
}
