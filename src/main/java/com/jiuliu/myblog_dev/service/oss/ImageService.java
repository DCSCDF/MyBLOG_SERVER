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
 * <p>直接从 OSS 流式传输图片数据到客户端，不在服务器端缓存完整数据。
 * 支持根据并发负载动态调整传输速度，防止后端 503。
 * 支持通过 OSS 图片处理参数实现动态缩放。</p>
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
     * 流式传输图片到响应（支持尺寸选择）
     *
     * <p>直接从 OSS 读取数据并写入 HTTP 响应，不在服务器端缓存完整图片。
     * 根据当前并发负载动态调整传输速度。
     * 如果尺寸不为 ORIGINAL，将使用 OSS 图片处理参数进行动态缩放。</p>
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

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            GetObjectRequest getObjectRequest = new GetObjectRequest(ossConfig.getBucket(), objectName);

            // 如果需要缩放，添加图片处理参数
            if (size != null && size != OSSConfig.ImageSize.ORIGINAL) {
                String processParam = buildOssProcessParam(size);
                getObjectRequest.setProcess(processParam);
                log.debug("[ImageService] 添加图片处理参数，hash=[{}], size=[{}], param=[{}]",
                        hash, size.getCode(), processParam);
            }

            log.debug("[ImageService] 开始获取 OSS 对象，bucket=[{}], objectName=[{}]", ossConfig.getBucket(), objectName);
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
                    if (size != null) {
                        log.debug("图片传输中 - hash=[{}], size=[{}], 已传输={}/{} bytes, 活跃请求={}, 限速={} KB/s",
                                hash, size.getCode(), totalBytesRead, contentLength, currentActive, recommendedSpeed);
                    }
                    lastLogTime = currentTime;
                }
            }

            outputStream.flush();
            if (size != null) {
                log.info("[ImageService] 图片传输完成 - hash=[{}], size=[{}], 大小={} bytes", hash, size.getCode(), contentLength);
            }
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
     * 构建 OSS 图片处理参数
     *
     * <p>使用 OSS 图片处理参数实现动态缩放。</p>
     *
     * @param size 图片尺寸规格
     * @return OSS 处理参数字符串，格式：image/resize,w_{width},h_{height},m_fill
     */
    private String buildOssProcessParam(OSSConfig.ImageSize size) {
        if (size == null || size == OSSConfig.ImageSize.ORIGINAL) {
            return null;
        }

        String resizeParam = size.getResizeParam();
        if (resizeParam == null || resizeParam.isBlank()) {
            return null;
        }

        // 格式: 200x200 -> image/resize,w_200,h_200,m_fill
        String[] parts = resizeParam.split("x");
        int width = Integer.parseInt(parts[0]);
        int height = parts.length > 1 ? Integer.parseInt(parts[1]) : width;

        return String.format("image/resize,w_%d,h_%d,m_fill", width, height);
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
