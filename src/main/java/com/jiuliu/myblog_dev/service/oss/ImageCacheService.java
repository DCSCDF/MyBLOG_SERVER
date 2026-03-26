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
 * UpdateTime: 2026/3/26
 */

package com.jiuliu.myblog_dev.service.oss;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jiuliu.myblog_dev.entity.oss.SysOssImage;
import com.jiuliu.myblog_dev.mapper.oss.SysOssImageMapper;
import com.jiuliu.myblog_dev.utils.cache.CacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 图片缓存服务
 *
 * <p>使用 Guava Cache 实现本地内存缓存，缓存从 OSS 获取的图片数据。</p>
 */
@Service
public class ImageCacheService {

    private static final Logger log = LoggerFactory.getLogger(ImageCacheService.class);

    /**
     * 图片缓存 - 有效期 30 分钟，最大 1000 条
     */
    private final Cache<String, byte[]> imageCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .recordStats()
            .build();

    private final SysOssImageMapper sysOssImageMapper;

    public ImageCacheService(SysOssImageMapper sysOssImageMapper) {
        this.sysOssImageMapper = sysOssImageMapper;
    }

    /**
     * 获取缓存的图片数据
     *
     * @param hash 图片哈希值
     * @return 缓存的图片字节数据，如果不存在返回 null
     */
    public byte[] getCachedImage(String hash) {
        String cacheKey = CacheUtil.CACHE_KEY_OSS_IMAGE + hash;
        return imageCache.getIfPresent(cacheKey);
    }

    /**
     * 缓存图片数据
     *
     * @param hash 图片哈希值
     * @param data 图片字节数据
     */
    public void cacheImage(String hash, byte[] data) {
        String cacheKey = CacheUtil.CACHE_KEY_OSS_IMAGE + hash;
        imageCache.put(cacheKey, data);
        log.debug("图片已缓存，hash=[{}]，大小={} bytes", hash, data.length);
    }

    /**
     * 根据哈希值获取图片记录
     *
     * @param hash 图片哈希值
     * @return 图片记录
     */
    public SysOssImage getImageByHash(String hash) {
        return sysOssImageMapper.selectByHash(hash);
    }

//    /**
//     * 清除指定图片的缓存
//     *
//     * @param hash 图片哈希值
//     */
//    public void evictCache(String hash) {
//        String cacheKey = CacheUtil.CACHE_KEY_OSS_IMAGE + hash;
//        imageCache.invalidate(cacheKey);
//        log.debug("图片缓存已清除，hash=[{}]", hash);
//    }

//    /**
//     * 获取缓存命中率统计
//     *
//     * @return 缓存命中率字符串
//     */
//    public String getCacheStats() {
//        return imageCache.stats().toString();
//    }
}
