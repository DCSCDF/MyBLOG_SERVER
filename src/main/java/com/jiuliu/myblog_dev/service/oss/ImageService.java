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
 * <p>
 * 直接从 OSS 流式传输图片数据到客户端，支持不同尺寸规格。
 * 使用 OSS 图片处理服务实现图片压缩，不在服务端内存中处理图片。
 * 支持根据并发负载动态调整传输速度，防止后端 503。
 * </p>
 *
 * <p>
 * <b>图片处理方式：</b>
 * </p>
 * <ul>
 * <li>使用 OSS 图片处理参数直接请求压缩后的图片</li>
 * <li>不在服务端内存中缓存或处理图片</li>
 * <li>所有图片通过流式传输直接返回给客户端</li>
 * </ul>
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
     * <p>
     * 使用 OSS 图片处理服务直接获取指定尺寸的图片，通过流式传输返回给客户端。
     * 不在服务端内存中处理或缓存图片。
     * </p>
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
            // 使用 OSS 图片处理服务获取指定尺寸的图片
            return streamImageWithOSSProcess(hash, size, ossClient, objectName, contentType, response);
        } catch (Exception e) {
            log.error("[ImageService] 图片传输异常，hash=[{}]：{}，异常类型={}",
                    hash, e.getMessage(), e.getClass().getSimpleName(), e);
            return false;
        }
    }

    /**
     * 使用 OSS 图片处理服务流式传输图片
     *
     * <p>根据尺寸规格，使用 OSS 的图片处理参数直接获取处理后的图片。
     * 所有图片通过流式传输直接返回给客户端，不在服务端内存中缓存。</p>
     *
     * @param hash        图片哈希值
     * @param size        图片尺寸规格
     * @param ossClient   OSS 客户端
     * @param objectName  OSS 对象名称
     * @param contentType MIME 类型
     * @param response    HTTP 响应
     * @return true 传输成功，false 传输失败
     */
    private boolean streamImageWithOSSProcess(String hash, OSSConfig.ImageSize size,
                                               OSS ossClient, String objectName,
                                               String contentType, HttpServletResponse response) {
        InputStream inputStream = null;
        String sizeCode = size.getCode();

        try {
            GetObjectRequest getObjectRequest = new GetObjectRequest(ossConfig.getBucket(), objectName);

            // 如果不是原图，添加 OSS 图片处理参数
            if (size != OSSConfig.ImageSize.ORIGINAL) {
                String processParam = buildOSSProcessParam(size);
                if (processParam != null && !processParam.isEmpty()) {
                    getObjectRequest.setProcess(processParam);
                    log.debug("[ImageService] 使用 OSS 图片处理，hash=[{}], size=[{}], process=[{}]",
                            hash, sizeCode, processParam);
                }
            }

            OSSObject ossObject = ossClient.getObject(getObjectRequest);

            if (ossObject == null) {
                log.error("[ImageService] OSS 返回空对象，objectName=[{}], bucket=[{}]",
                        objectName, ossConfig.getBucket());
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

            log.info("[ImageService] 图片传输完成 - hash=[{}], size=[{}], 大小={} bytes",
                    hash, sizeCode, contentLength);
            return true;

        } catch (IOException e) {
            log.error("[ImageService] 图片传输 IO 异常，hash=[{}], size=[{}]：{}",
                    hash, sizeCode, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("[ImageService] 图片传输异常，hash=[{}], size=[{}]：{}",
                    hash, sizeCode, e.getMessage(), e);
            return false;
        } finally {
            closeQuietly(inputStream);
        }
    }

    /**
     * 构建 OSS 图片处理参数
     *
     * <p>
     * 根据 OSSConfig.ImageSize 构建 OSS 原始图片处理参数字符串。
     * 使用 OSS 原生的 image/resize 参数格式，无需在 OSS 控制台预先配置样式。
     * </p>
     *
     * @param size 图片尺寸规格
     * @return OSS 图片处理参数字符串（x-oss-process 格式），原图返回空字符串
     */
    private String buildOSSProcessParam(OSSConfig.ImageSize size) {
        if (size == null || size == OSSConfig.ImageSize.ORIGINAL) {
            return "";
        }

        String resizeParam = size.getResizeParam();
        if (resizeParam == null || resizeParam.isBlank()) {
            return "";
        }

        // 使用 OSS 原生图片处理参数（等比缩放到指定宽度，保持宽高比）
        // 格式示例：image/resize,w_256,m_lfit —— 等比缩放至宽度256px
        // 参考：https://help.aliyun.com/document_detail/44688.html
        return "image/resize,w_" + resizeParam + ",m_lfit";
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
