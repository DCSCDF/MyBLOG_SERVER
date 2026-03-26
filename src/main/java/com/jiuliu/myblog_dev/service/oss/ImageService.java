/*
 * [ImageService.java]
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

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.jiuliu.myblog_dev.config.business.OSSConfig;
import com.jiuliu.myblog_dev.entity.oss.SysOssImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * 图片获取服务
 *
 * <p>负责从 OSS 获取图片，支持本地缓存。</p>
 */
@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    private final ImageCacheService imageCacheService;
    private final OSSConfig ossConfig;

    public ImageService(ImageCacheService imageCacheService, OSSConfig ossConfig) {
        this.imageCacheService = imageCacheService;
        this.ossConfig = ossConfig;
    }

    /**
     * 图片获取结果
     */
    public record ImageResult(byte[] data, String extension, boolean found) {
    }

    /**
     * 根据哈希值获取图片
     *
     * <p>流程：
     * 1. 先检查本地缓存（30分钟有效期）
     * 2. 缓存未命中则从数据库查询图片记录
     * 3. 根据记录从 OSS 下载图片
     * 4. 存入本地缓存后返回</p>
     *
     * @param hash 图片哈希值（MD5）
     * @return 图片结果，包含图片数据和扩展名
     */
    public ImageResult getImageByHash(String hash) {
//        log.debug("获取图片，hash=[{}]", hash);

        // 1. 检查缓存
        byte[] cachedImage = imageCacheService.getCachedImage(hash);
        if (cachedImage != null) {
//            log.debug("图片命中缓存，hash=[{}]，大小={} bytes", hash, cachedImage.length);
            String extension = getExtensionFromCache(hash);
            return new ImageResult(cachedImage, extension, true);
        }

        // 2. 从数据库查询图片记录
        SysOssImage imageRecord = imageCacheService.getImageByHash(hash);
        if (imageRecord == null) {
            log.warn("图片记录不存在，hash=[{}]", hash);
            return new ImageResult(null, null, false);
        }

        // 3. 从 OSS 下载图片
        OSS ossClient = ossConfig.getOssClient();
        if (ossClient == null) {
            log.error("OSS 客户端不可用");
            return new ImageResult(null, null, false);
        }

        String objectName = imageRecord.getObjectName();
        String extension = getExtensionFromObjectName(objectName);

        try {
//            log.debug("从 OSS 下载图片，objectName=[{}]", objectName);

            OSSObject ossObject = ossClient.getObject(ossConfig.getBucket(), objectName);
            if (ossObject == null) {
                log.error("OSS 返回空对象，objectName=[{}]", objectName);
                return new ImageResult(null, null, false);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream inputStream = ossObject.getObjectContent()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
            }

            byte[] imageBytes = baos.toByteArray();
//            log.debug("图片下载成功，hash=[{}]，大小={} bytes", hash, imageBytes.length);

            // 4. 存入缓存
            imageCacheService.cacheImage(hash, imageBytes);

            return new ImageResult(imageBytes, extension, true);
        } catch (Exception e) {
            log.error("从 OSS 获取图片失败：{}", e.getMessage(), e);
            return new ImageResult(null, null, false);
        }
    }

    /**
     * 从缓存中获取扩展名（通过查询数据库）
     */
    private String getExtensionFromCache(String hash) {
        SysOssImage record = imageCacheService.getImageByHash(hash);
        if (record != null) {
            return getExtensionFromObjectName(record.getObjectName());
        }
        return "";
    }

    /**
     * 从对象名获取文件扩展名
     */
    private String getExtensionFromObjectName(String objectName) {
        if (objectName == null || !objectName.contains(".")) {
            return "";
        }
        return objectName.substring(objectName.lastIndexOf(".") + 1).toLowerCase();
    }
}
