/*
 * [PublicCategoryServiceImpl.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/23 06:57
 */

package com.jiuliu.myblog_dev.service.blog.category;

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jiuliu.myblog_dev.dto.blog.publicity.PublicCategoryResponseDTO;
import com.jiuliu.myblog_dev.entity.blog.category.SysCategory;
import com.jiuliu.myblog_dev.mapper.blog.category.SysCategoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 公共分类 Service 实现类
 */
@Service
public class PublicCategoryServiceImpl implements PublicCategoryService {

    private static final Logger log = LoggerFactory.getLogger(PublicCategoryServiceImpl.class);

    /**
     * 公共分类列表缓存
     * 缓存时间：30分钟
     */
    private final Cache<String, List<PublicCategoryResponseDTO>> publicCategoryListCache = CacheBuilder.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    /**
     * 单个分类详情缓存
     * 缓存时间：30分钟
     */
    private final Cache<Long, PublicCategoryResponseDTO> publicCategoryCache = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    private static final String CACHE_KEY_ALL_CATEGORIES = "all_visible_categories";

    private final SysCategoryMapper categoryMapper;

    public PublicCategoryServiceImpl(SysCategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public SaResult getVisibleCategories() {
        try {
            // 尝试从缓存获取
            List<PublicCategoryResponseDTO> cached = publicCategoryListCache.getIfPresent(CACHE_KEY_ALL_CATEGORIES);
            if (cached != null) {
                log.debug("从缓存获取公共分类列表");
                return SaResult.data(cached);
            }

            // 查询所有未隐藏的分类
            LambdaQueryWrapper<SysCategory> wrapper = new LambdaQueryWrapper<SysCategory>()
                    .eq(SysCategory::getHidden, false)
                    .orderByDesc(SysCategory::getSortOrder)
                    .orderByAsc(SysCategory::getCreateTime);

            List<SysCategory> categories = categoryMapper.selectList(wrapper);

            // 转换为响应DTO
            List<PublicCategoryResponseDTO> result = categories.stream()
                    .map(this::toResponseDTO)
                    .toList();

            // 存入缓存
            publicCategoryListCache.put(CACHE_KEY_ALL_CATEGORIES, result);

            return SaResult.data(result);
        } catch (Exception e) {
            log.error("获取公共分类列表异常", e);
            return SaResult.error("获取分类列表失败").setCode(500);
        }
    }

    @Override
    public SaResult getCategoryById(Long categoryId) {
        try {
            if (categoryId == null) {
                return SaResult.error("分类ID不能为空").setCode(400);
            }

            // 尝试从缓存获取
            PublicCategoryResponseDTO cached = publicCategoryCache.getIfPresent(categoryId);
            if (cached != null) {
                log.debug("从缓存获取公共分类详情，id={}", categoryId);
                return SaResult.data(cached);
            }

            // 查询分类（仅未隐藏的）
            SysCategory category = categoryMapper.selectOne(
                    new LambdaQueryWrapper<SysCategory>()
                            .eq(SysCategory::getId, categoryId)
                            .eq(SysCategory::getHidden, false)
            );

            if (category == null) {
                return SaResult.error("分类不存在或已隐藏").setCode(404);
            }

            PublicCategoryResponseDTO response = toResponseDTO(category);

            // 存入缓存
            publicCategoryCache.put(categoryId, response);

            return SaResult.data(response);
        } catch (Exception e) {
            log.error("获取公共分类详情异常，id={}", categoryId, e);
            return SaResult.error("获取分类详情失败").setCode(500);
        }
    }

    @Override
    public void clearPublicCategoryCache() {
        publicCategoryListCache.invalidateAll();
        publicCategoryCache.invalidateAll();
        log.debug("公共分类缓存已清除");
    }

    /**
     * 将实体转换为公共分类响应DTO
     */
    private PublicCategoryResponseDTO toResponseDTO(SysCategory category) {
        PublicCategoryResponseDTO dto = new PublicCategoryResponseDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setSortOrder(category.getSortOrder());
        return dto;
    }
}
