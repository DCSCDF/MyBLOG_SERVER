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
 * UpdateTime: 2026/4/1
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
 * <p>直接从 OSS 流式传输图片数据到客户端，不在服务器端缓存完整数据。
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

    public ImageService(SysOssImageMapper sysOssImageMapper,
                        OSSConfig ossConfig,
                        DynamicRateLimitService dynamicRateLimitService) {
        this.sysOssImageMapper = sysOssImageMapper;
        this.ossConfig = ossConfig;
        this.dynamicRateLimitService = dynamicRateLimitService;
    }

    /**
     * 图片元数据
     */
    public record ImageMeta(String extension, long contentLength, String contentType) {
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

        return new ImageMeta(extension, imageRecord.getFileSize(), contentType);
    }

    /**
     * 流式传输图片到响应
     *
     * <p>直接从 OSS 读取数据并写入 HTTP 响应，不在服务器端缓存完整图片。
     * 根据当前并发负载动态调整传输速度。</p>
     *
     * @param hash     图片哈希值
     * @param response HTTP 响应
     * @return true 传输成功，false 图片不存在或传输失败
     */
    public boolean streamImage(String hash, HttpServletResponse response) {
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

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            log.debug("[ImageService] 开始获取 OSS 对象，bucket=[{}], objectName=[{}]", ossConfig.getBucket(), objectName);
            GetObjectRequest getObjectRequest = new GetObjectRequest(ossConfig.getBucket(), objectName);
            OSSObject ossObject = ossClient.getObject(getObjectRequest);

            if (ossObject == null) {
                log.error("[ImageService] OSS 返回空对象，objectName=[{}], bucket=[{}]", objectName, ossConfig.getBucket());
                return false;
            }

            ObjectMetadata metadata = ossObject.getObjectMetadata();
            long contentLength = metadata.getContentLength();
            log.debug("[ImageService] OSS 对象获取成功，contentLength=[{}]", contentLength);

            response.setContentType(contentType);
            response.setHeader("Content-Length", String.valueOf(contentLength));
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            inputStream = ossObject.getObjectContent();
            outputStream = response.getOutputStream();

            int recommendedSpeed = dynamicRateLimitService.getRecommendedTransferSpeed();
            int bufferSize = calculateBufferSize(recommendedSpeed);

            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            long totalBytesRead = 0;
            long lastLogTime = System.currentTimeMillis();

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastLogTime >= 1000) {
                    int currentActive = dynamicRateLimitService.getActiveRequestCount();
                    log.debug("图片传输中 - hash=[{}], 已传输={}/{} bytes, 活跃请求={}, 限速={} KB/s",
                            hash, totalBytesRead, contentLength, currentActive, recommendedSpeed);
                    lastLogTime = currentTime;
                }
            }

            outputStream.flush();
            log.info("[ImageService] 图片传输完成 - hash=[{}], 大小={} bytes", hash, contentLength);
            return true;

        } catch (IOException e) {
            log.error("[ImageService] 图片传输 IO 异常，hash=[{}]：{}，异常类型={}", hash, e.getMessage(), e.getClass().getSimpleName(), e);
            return false;
        } catch (Exception e) {
            log.error("[ImageService] 图片传输异常，hash=[{}]：{}，异常类型={}", hash, e.getMessage(), e.getClass().getSimpleName(), e);
            return false;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
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
