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
 * UpdateTime: 2026/4/4
 */

package com.jiuliu.myblog_dev.service.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.jiuliu.myblog_dev.config.business.OSSConfig;
import com.jiuliu.myblog_dev.entity.oss.SysOssImage;
import com.jiuliu.myblog_dev.mapper.oss.SysOssImageMapper;
import com.jiuliu.myblog_dev.utils.rateLimit.DynamicRateLimitService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 图片获取服务
 *
 * <p>直接从 OSS 流式传输图片数据到客户端，支持图片压缩和缓存功能。
 * 缓存压缩后的图片数据，减少 OSS 请求压力和网络传输量。
 * 支持根据并发负载动态调整传输速度，防止后端 503。</p>
 */
@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    /**
     * 默认传输缓冲区大小（32KB）
     */
    private static final int DEFAULT_BUFFER_SIZE = 32 * 1024;

    /**
     * 最大传输缓冲区大小（64KB）
     */
    private static final int MAX_BUFFER_SIZE = 64 * 1024;

    private final SysOssImageMapper sysOssImageMapper;
    private final OSSConfig ossConfig;
    private final DynamicRateLimitService dynamicRateLimitService;
    private final ImageCacheService imageCacheService;

    public ImageService(SysOssImageMapper sysOssImageMapper,
                        OSSConfig ossConfig,
                        DynamicRateLimitService dynamicRateLimitService,
                        ImageCacheService imageCacheService) {
        this.sysOssImageMapper = sysOssImageMapper;
        this.ossConfig = ossConfig;
        this.dynamicRateLimitService = dynamicRateLimitService;
        this.imageCacheService = imageCacheService;
    }

    /**
     * 图片元数据
     *
     * @param extension     文件扩展名
     * @param contentLength 文件大小
     * @param contentType   MIME类型
     * @param objectName    OSS对象名称
     */
    public record ImageMeta(String extension, long contentLength, String contentType, String objectName) {
    }

    /**
     * 检查图片是否存在
     *
     * @param hash 图片哈希值
     * @return 图片元数据，不存在返回 null
     */
    public ImageMeta getImageMeta(String hash) {
        SysOssImage imageRecord = sysOssImageMapper.selectByHash(hash);
        if (imageRecord == null) {
            log.warn("图片记录不存在，hash=[{}]", hash);
            return null;
        }

        String objectName = imageRecord.getObjectName();
        String extension = getExtensionFromObjectName(objectName);
        String contentType = getContentType(extension);

        return new ImageMeta(extension, imageRecord.getFileSize(), contentType, objectName);
    }

    /**
     * 流式传输图片到响应（支持尺寸选择和缓存压缩）
     *
     * <p>优先从缓存获取压缩后的图片数据，缓存未命中时从 OSS 获取并压缩后缓存。
     * 支持根据并发负载动态调整传输速度。
     * 当尺寸不为 ORIGINAL 时，会使用服务端压缩处理。</p>
     *
     * @param hash     图片哈希值
     * @param size     图片尺寸规格
     * @param response HTTP 响应
     * @return true 传输成功，false 图片不存在或传输失败
     */
    public boolean streamImage(String hash, OSSConfig.ImageSize size, HttpServletResponse response) {
        SysOssImage imageRecord = sysOssImageMapper.selectByHash(hash);
        if (imageRecord == null) {
            log.error("[ImageService] 图片记录不存在，hash=[{}]", hash);
            return false;
        }

        String objectName = imageRecord.getObjectName();
        String extension = getExtensionFromObjectName(objectName);
        String contentType = getContentType(extension);

        OSS ossClient = ossConfig.getOssClient();
        if (ossClient == null) {
            log.error("[ImageService] OSS 客户端不可用，ossConfig.getOssClient() 返回 null");
            return false;
        }

        try {
            // 如果需要压缩处理（非原图），优先使用缓存
            if (size != null && size != OSSConfig.ImageSize.ORIGINAL) {
                return streamCompressedImage(hash, size, ossClient, objectName,
                        imageRecord.getFileSize(), contentType, response);
            }

            // 原图直接流式传输（不压缩）
            return streamOriginalImage(hash, ossClient, objectName, contentType, response);

        } catch (Exception e) {
            log.error("[ImageService] 图片传输异常，hash=[{}]：{}，异常类型={}",
                    hash, e.getMessage(), e.getClass().getSimpleName(), e);
            return false;
        }
    }

    /**
     * 流式传输压缩后的图片（使用缓存）
     */
    private boolean streamCompressedImage(String hash, OSSConfig.ImageSize size,
                                          OSS ossClient, String objectName,
                                          long originalSize, String contentType,
                                          HttpServletResponse response) {
        try {
            // 先尝试从缓存获取（不需要输入流）
            byte[] cachedImage = imageCacheService.getCompressedImage(hash, size, null, originalSize, contentType);

            if (cachedImage != null) {
                // 缓存命中，直接返回
                log.debug("[ImageService] 缓存命中，hash=[{}], size=[{}], 大小=[{} bytes]",
                        hash, size.getCode(), cachedImage.length);

                response.setContentType(contentType);
                response.setHeader("Content-Length", String.valueOf(cachedImage.length));
                response.setHeader("Accept-Ranges", "bytes");
                response.setHeader("Cache-Control", "private, max-age=600");
                response.setHeader("X-Cache", "HIT");

                try (OutputStream outputStream = response.getOutputStream()) {
                    outputStream.write(cachedImage);
                    outputStream.flush();
                }

                log.info("[ImageService] 图片传输完成（缓存） - hash=[{}], size=[{}], 大小={} bytes",
                        hash, size.getCode(), cachedImage.length);
                return true;
            }

            // 缓存未命中，从 OSS 获取并压缩
            log.debug("[ImageService] 缓存未命中，hash=[{}], size=[{}]，从 OSS 获取并压缩", hash, size.getCode());

            InputStream inputStream = null;
            try {
                GetObjectRequest getObjectRequest = new GetObjectRequest(ossConfig.getBucket(), objectName);
                inputStream = ossClient.getObject(getObjectRequest).getObjectContent();

                // 使用缓存服务压缩并缓存
                cachedImage = imageCacheService.getCompressedImage(hash, size, inputStream, originalSize, contentType);
            } finally {
                closeQuietly(inputStream);
            }

            if (cachedImage == null) {
                log.error("[ImageService] 图片压缩失败，hash=[{}], size=[{}]", hash, size.getCode());
                return false;
            }

            response.setContentType(contentType);
            response.setHeader("Content-Length", String.valueOf(cachedImage.length));
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Cache-Control", "private, max-age=600");
            response.setHeader("X-Cache", "MISS");

            try (OutputStream outputStream = response.getOutputStream()) {
                outputStream.write(cachedImage);
                outputStream.flush();
            }

            log.info("[ImageService] 图片传输完成（压缩） - hash=[{}], size=[{}], 大小={} bytes, 原图={} bytes",
                    hash, size.getCode(), cachedImage.length, originalSize);
            return true;

        } catch (Exception e) {
            log.error("[ImageService] 图片压缩传输异常，hash=[{}]：{}", hash, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 流式传输原图（不压缩）
     */
    private boolean streamOriginalImage(String hash, OSS ossClient, String objectName,
                                        String contentType, HttpServletResponse response) {
        InputStream inputStream = null;

        try {
            GetObjectRequest getObjectRequest = new GetObjectRequest(ossConfig.getBucket(), objectName);
            OSSObject ossObject = ossClient.getObject(getObjectRequest);

            if (ossObject == null) {
                log.error("[ImageService] OSS 返回空对象，objectName=[{}], bucket=[{}]", objectName, ossConfig.getBucket());
                return false;
            }

            ObjectMetadata metadata = ossObject.getObjectMetadata();
            long contentLength = metadata.getContentLength();

            response.setContentType(contentType);
            response.setHeader("Content-Length", String.valueOf(contentLength));
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Cache-Control", "private, max-age=3600");

            inputStream = ossObject.getObjectContent();

            int recommendedSpeed = dynamicRateLimitService.getRecommendedTransferSpeed();
            int bufferSize = calculateBufferSize(recommendedSpeed);

            byte[] buffer = new byte[bufferSize];
            int bytesRead;

            try (OutputStream outputStream = response.getOutputStream()) {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }

            log.info("[ImageService] 原图传输完成 - hash=[{}], 大小={} bytes", hash, contentLength);
            return true;

        } catch (IOException e) {
            log.error("[ImageService] 原图传输 IO 异常，hash=[{}]：{}", hash, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("[ImageService] 原图传输异常，hash=[{}]：{}", hash, e.getMessage(), e);
            return false;
        } finally {
            closeQuietly(inputStream);
        }
    }

    /**
     * 安全关闭流
     */
    private void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                log.warn("[ImageService] 关闭流时发生异常：{}", e.getMessage());
            }
        }
    }

    /**
     * 根据推荐速度计算缓冲区大小
     */
    private int calculateBufferSize(int recommendedSpeedKBps) {
        if (recommendedSpeedKBps >= 512) {
            return MAX_BUFFER_SIZE;
        } else if (recommendedSpeedKBps >= 256) {
            return DEFAULT_BUFFER_SIZE;
        } else {
            return 16 * 1024;
        }
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

    /**
     * 根据扩展名获取 MIME 类型
     */
    private String getContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            case "svg" -> "image/svg+xml";
            case "ico" -> "image/x-icon";
            default -> "application/octet-stream";
        };
    }
}
