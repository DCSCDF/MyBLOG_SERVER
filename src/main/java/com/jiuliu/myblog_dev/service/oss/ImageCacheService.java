/*
 * [ImageCacheService.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/4/9
 */

package com.jiuliu.myblog_dev.service.oss;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Weigher;
import com.jiuliu.myblog_dev.config.business.OSSConfig;
import com.jiuliu.myblog_dev.utils.image.ImageUtil;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 图片压缩缓存服务
 *
 * <p>提供服务端图片压缩和缓存功能，支持：</p>
 * <ul>
 *   <li>从 OSS 获取原图后进行压缩处理</li>
 *   <li>使用内存缓存压缩后的图片数据</li>
 *   <li>缓存大小限制：100MB（可通过代码修改）</li>
 *   <li>缓存过期时间：10分钟</li>
 * </ul>
 */
@Service
public class ImageCacheService {

    private static final Logger log = LoggerFactory.getLogger(ImageCacheService.class);

    /**
     * 图片缓存最大大小（50MB，更保守设置）
     * 单位：字节
     */
    private static final long MAX_CACHE_SIZE_BYTES = 50 * 1024 * 1024;

    /**
     * 缓存过期时间（5分钟，更快过期释放内存）
     */
    private static final int CACHE_EXPIRE_MINUTES = 5;

    /**
     * 图片质量（压缩质量 0.75 表示 75%，更小的文件）
     */
    private static final float COMPRESSION_QUALITY = 0.75f;

    /**
     * 图片缓存
     * 使用 Weigher 接口限制缓存总大小，缓存键格式：hash_sizeCode
     */
    private final Cache<String, byte[]> imageCache;

    /**
     * 并发控制：防止同一张图片同时被多个请求压缩
     */
    private final ConcurrentHashMap<String, Object> compressionLocks;

    public ImageCacheService() {
        this.imageCache = CacheBuilder.newBuilder()
                .maximumWeight(MAX_CACHE_SIZE_BYTES)
                .weigher(new ImageCacheWeigher())
                .expireAfterWrite(CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .concurrencyLevel(4)
                .softValues()
                .recordStats()
                .build();
        this.compressionLocks = new ConcurrentHashMap<>();
        log.info("图片压缩缓存服务初始化完成，最大缓存大小=[{} MB]，过期时间=[{} 分钟]",
                MAX_CACHE_SIZE_BYTES / (1024 * 1024), CACHE_EXPIRE_MINUTES);
    }

    /**
     * 获取压缩后的图片数据
     *
     * <p>
     * 如果缓存中存在，直接返回缓存数据。
     * 如果不存在，从 OSS 获取原图，进行压缩处理后存入缓存并返回。
     * 如果压缩失败，会返回原图数据。
     * </p>
     *
     * @param hash         图片哈希值
     * @param size         图片尺寸规格
     * @param inputStream  OSS 图片输入流（如果缓存未命中，需要传入此流进行压缩）
     * @param originalSize 原图大小
     * @param contentType  图片 MIME 类型
     * @return 图片字节数组（压缩后的或原图），如果无法获取任何数据则返回 null
     */
    public byte[] getCompressedImage(String hash, OSSConfig.ImageSize size,
                                     InputStream inputStream, long originalSize, String contentType) {
        String cacheKey = buildCacheKey(hash, size);
        String sizeCode = (size != null) ? size.getCode() : "o";

        // 尝试从缓存获取（双重检查）
        byte[] cached = imageCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("[缓存命中] hash=[{}], size=[{}], 缓存大小=[{} bytes]",
                    hash, sizeCode, cached.length);
            return cached;
        }

        // 如果是原图尺寸或没有输入流，直接读取原图
        if (size == null || size == OSSConfig.ImageSize.ORIGINAL || inputStream == null) {
            if (inputStream != null) {
                log.debug("[直接返回原图] hash=[{}], size=[{}]", hash, sizeCode);
                return readInputStream(inputStream);
            }
            log.warn("[缓存未命中且无输入流] hash=[{}], size=[{}]", hash, sizeCode);
            return null;
        }

        // 缓存未命中，进行压缩处理
        log.debug("[缓存未命中] hash=[{}], size=[{}], 开始压缩", hash, sizeCode);

        // 使用并发锁防止同一张图片被重复压缩
        Object lock = compressionLocks.computeIfAbsent(cacheKey, k -> new Object());
        synchronized (lock) {
            // 再次检查缓存，防止在等待锁期间其他线程已经完成压缩
            cached = imageCache.getIfPresent(cacheKey);
            if (cached != null) {
                log.debug("[缓存二次命中] hash=[{}], size=[{}]", hash, sizeCode);
                compressionLocks.remove(cacheKey);
                return cached;
            }

            try {
                byte[] compressed = compressImage(inputStream, size, contentType);
                if (compressed != null && compressed.length > 0) {
                    // 存入缓存
                    imageCache.put(cacheKey, compressed);
                    log.info("[图片压缩完成] hash=[{}], size=[{}], 原图大小=[{} bytes], 压缩后=[{} bytes], 压缩率=[{}%]",
                            hash, sizeCode, originalSize, compressed.length,
                            String.format("%.1f", (1 - (double) compressed.length / originalSize) * 100));
                    printCacheStats();
                }
                compressionLocks.remove(cacheKey);
                return compressed;
            } catch (Exception e) {
                log.error("[图片压缩失败] hash=[{}], size=[{}]：{}", hash, sizeCode, e.getMessage(), e);
                compressionLocks.remove(cacheKey);
                // 压缩失败，重新读取并返回原图
                try {
                    // 注意：此时输入流可能已被读取过，这里需要特殊处理
                    // 但在实际场景中，我们已经在 compressImage 中保存了 originalBytes，
                    // 所以那里已经处理了返回原图的情况，这里主要是兜底
                    log.warn("[压缩异常兜底] hash=[{}], size=[{}], 返回 null，调用方需回退到原图",
                            hash, sizeCode);
                    return null;
                } catch (Exception ex) {
                    log.error("[压缩异常兜底失败] hash=[{}]：{}", hash, ex.getMessage(), ex);
                    return null;
                }
            }
        }
    }

    /**
     * 压缩图片
     *
     * @param inputStream 输入流
     * @param size        目标尺寸
     * @param contentType MIME 类型
     * @return 压缩后的字节数组，如果压缩失败则返回原图
     */
    private byte[] compressImage(InputStream inputStream, OSSConfig.ImageSize size, String contentType) {
        if (size == null || size == OSSConfig.ImageSize.ORIGINAL) {
            return readInputStream(inputStream);
        }

        // 首先一次性读取原图到字节数组（只读取一次）
        byte[] originalBytes = readInputStream(inputStream);
        if (originalBytes == null || originalBytes.length == 0) {
            log.warn("[compressImage] 无法读取输入流，返回 null");
            return null;
        }

        try {
            int targetSize = parseTargetSize(size);
            String outputFormat = getOutputFormat(contentType);

            // 从 contentType 推断文件扩展名
            String extension = getExtensionFromContentType(contentType);

            // 验证图片格式（检查文件头），而不仅仅是依赖表面扩展名
            if (!ImageUtil.isValidFormat(originalBytes, extension)) {
                log.warn("[图片格式校验失败]：无法识别的图片格式，直接返回原图，contentType=[{}]", contentType);
                return originalBytes;
            }

            // 尝试直接压缩，如果压缩后更小就返回，否则返回原图
            byte[] result;
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(originalBytes.length / 2)) {
                Thumbnails.of(new ByteArrayInputStream(originalBytes))
                        .size(targetSize, targetSize)
                        .keepAspectRatio(true)
                        .outputQuality(COMPRESSION_QUALITY)
                        .outputFormat(outputFormat)
                        .toOutputStream(outputStream);
                result = outputStream.toByteArray();
            }

            if (result.length > 0 && result.length < originalBytes.length) {
                return result;
            } else {
                log.debug("[无需压缩] 原图更小或压缩失败，直接返回原图，原图=[{} bytes], 压缩后=[{} bytes]",
                                originalBytes.length, result.length);
                return originalBytes;
            }

        } catch (Throwable t) {
            // 捕获所有 Throwable，包括 Error，确保不会崩溃
            log.error("[图片压缩异常]：{}", t.getMessage(), t);
            return originalBytes; // 任何异常都返回原图
        }
    }

    /**
     * 根据 Content-Type 推断文件扩展名
     */
    private String getExtensionFromContentType(String contentType) {
        if (contentType == null) {
            return "jpg";
        }
        return switch (contentType.toLowerCase()) {
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "image/bmp" -> "bmp";
            default -> "jpg";
        };
    }

    /**
     * 解析目标尺寸
     * 返回宽度值，高度会根据长宽比自动计算
     */
    private int parseTargetSize(OSSConfig.ImageSize size) {
        if (size == null || size.getResizeParam() == null) {
            return 1080;
        }
        String resizeParam = size.getResizeParam();
        if (resizeParam.isBlank()) {
            return 1080;
        }
        return Integer.parseInt(resizeParam);
    }

    /**
     * 根据 MIME 类型获取输出格式
     */
    private String getOutputFormat(String contentType) {
        if (contentType == null) {
            return "jpeg";
        }
        return switch (contentType.toLowerCase()) {
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "jpeg";
        };
    }

    /**
     * 从输入流读取所有字节
     */
    private byte[] readInputStream(InputStream inputStream) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("[读取输入流异常]：{}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(String hash, OSSConfig.ImageSize size) {
        return hash + "_" + (size != null ? size.getCode() : "o");
    }

    /**
     * 打印缓存统计信息
     */
    private void printCacheStats() {
        var stats = imageCache.stats();
        log.debug("[缓存统计] hitCount=[{}], missCount=[{}], hitRate=[{}%]",
                stats.hitCount(), stats.missCount(),
                String.format("%.2f", stats.hitRate() * 100));
    }

    /**
     * 清除所有缓存
     */
    @SuppressWarnings("unused")
    public void clearCache() {
        imageCache.invalidateAll();
        log.info("图片压缩缓存已全部清除");
    }

    /**
     * 获取缓存统计信息
     */
    @SuppressWarnings("unused")
    public String getCacheStats() {
        var stats = imageCache.stats();
        return String.format("缓存统计 - 命中次数=%d, 未命中次数=%d, 命中率=%.2f%%, 当前条目数=%d",
                stats.hitCount(), stats.missCount(), stats.hitRate() * 100, imageCache.size());
    }

    /**
     * 图片缓存称重器
     * 用于限制缓存总大小
     */
    private static class ImageCacheWeigher implements Weigher<String, byte[]> {
        @Override
        public int weigh(String key, byte[] value) {
            return value != null ? value.length : 0;
        }
    }
}
