/*
 * [PublicSeoServiceImpl.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/27
 */

package com.jiuliu.myblog_dev.service.seo;

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jiuliu.myblog_dev.dto.seo.PublicSeoResponseDTO;
import com.jiuliu.myblog_dev.entity.seo.SysSeo;
import com.jiuliu.myblog_dev.mapper.seo.SysSeoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 公共SEO Service实现类 - 无需登录即可访问
 */
@Service
public class PublicSeoServiceImpl implements PublicSeoService {

    private static final Logger log = LoggerFactory.getLogger(PublicSeoServiceImpl.class);

    /**
     * SEO配置缓存 - key为 pageType_pageId，value为PublicSeoResponseDTO
     * 缓存时间：30分钟
     */
    private final Cache<String, PublicSeoResponseDTO> seoCache = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    private final SysSeoMapper sysSeoMapper;

    public PublicSeoServiceImpl(SysSeoMapper sysSeoMapper) {
        this.sysSeoMapper = sysSeoMapper;
    }

    @Override
    public SaResult getSeoByPageType(String pageType, Long pageId) {
        try {
            if (pageType == null || pageType.trim().isEmpty()) {
                return SaResult.error("页面类型不能为空").setCode(400);
            }

            // 构建缓存键
            String cacheKey = buildCacheKey(pageType, pageId);

            // 尝试从缓存获取
            PublicSeoResponseDTO cached = seoCache.getIfPresent(cacheKey);
            if (cached != null) {
                log.debug("从缓存获取SEO配置，key={}", cacheKey);
                return SaResult.data(cached);
            }

            // 构建查询条件
            LambdaQueryWrapper<SysSeo> wrapper = new LambdaQueryWrapper<SysSeo>()
                    .eq(SysSeo::getPageType, pageType.trim())
                    .eq(SysSeo::getIsDeleted, 0);

            // 如果传入了pageId，优先匹配精确组合
            if (pageId != null) {
                wrapper.and(w -> w
                        .eq(SysSeo::getPageId, pageId)
                        .or()
                        .isNull(SysSeo::getPageId));
            } else {
                wrapper.isNull(SysSeo::getPageId);
            }

            // 按 pageId 非空优先排序，确保精确匹配优先
            wrapper.orderByDesc(SysSeo::getPageId);

            SysSeo seo = sysSeoMapper.selectOne(wrapper);

            if (seo == null) {
                log.debug("未找到SEO配置，pageType={}, pageId={}", pageType, pageId);
                return SaResult.error("未找到该页面类型的SEO配置").setCode(404);
            }

            // 转换为响应DTO
            PublicSeoResponseDTO responseDTO = toResponseDTO(seo);

            // 存入缓存
            seoCache.put(cacheKey, responseDTO);

            return SaResult.data(responseDTO);
        } catch (Exception e) {
            log.error("获取SEO配置异常，pageType={}, pageId={}", pageType, pageId, e);
            return SaResult.error("获取SEO配置失败").setCode(500);
        }
    }

    @Override
    public void clearPublicSeoCache() {
        seoCache.invalidateAll();
        log.debug("公共SEO缓存已清除");
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(String pageType, Long pageId) {
        return "seo:" + pageType + ":" + (pageId != null ? pageId : "null");
    }

    /**
     * 实体转换为响应DTO
     */
    private PublicSeoResponseDTO toResponseDTO(SysSeo seo) {
        PublicSeoResponseDTO dto = new PublicSeoResponseDTO();
        dto.setId(seo.getId());
        dto.setPageType(seo.getPageType());
        dto.setPageId(seo.getPageId());
        dto.setTitle(seo.getTitle());
        dto.setKeywords(seo.getKeywords());
        dto.setDescription(seo.getDescription());
        dto.setOgTitle(seo.getOgTitle());
        dto.setOgDescription(seo.getOgDescription());
        dto.setOgImage(seo.getOgImage());
        dto.setOgType(seo.getOgType());
        dto.setCanonicalUrl(seo.getCanonicalUrl());
        dto.setRobots(seo.getRobots());
        return dto;
    }
}
