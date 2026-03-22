/*
 * [PublicArticleServiceImpl.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/14
 */

package com.jiuliu.myblog_dev.service.blog;

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jiuliu.myblog_dev.dto.blog.publicity.PagePublicArticleDTO;
import com.jiuliu.myblog_dev.dto.blog.publicity.PagePublicArticleResponseDTO;
import com.jiuliu.myblog_dev.dto.blog.publicity.PublicArticleResponseDTO;
import com.jiuliu.myblog_dev.entity.blog.SysBlog;
import com.jiuliu.myblog_dev.entity.blog.category.SysCategory;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.mapper.blog.SysBlogMapper;
import com.jiuliu.myblog_dev.mapper.blog.category.SysCategoryMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.utils.cache.CacheUtil;
import com.jiuliu.myblog_dev.utils.markdown.MarkdownUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 公共文章 Service 实现类
 */
@Service
public class PublicArticleServiceImpl implements PublicArticleService {

    private static final Logger log = LoggerFactory.getLogger(PublicArticleServiceImpl.class);

    /**
     * 公共文章列表缓存 - 缓存公共文章列表
     * 缓存时间：30分钟
     */
    private final Cache<String, PagePublicArticleResponseDTO> publicArticleListCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    private final SysBlogMapper blogMapper;
    private final SysCategoryMapper categoryMapper;
    private final SysUserMapper userMapper;

    public PublicArticleServiceImpl(SysBlogMapper blogMapper,
                                    SysCategoryMapper categoryMapper,
                                    SysUserMapper userMapper) {
        this.blogMapper = blogMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
    }

    @Override
    public SaResult getPagePublicArticles(PagePublicArticleDTO dto) {
        try {
            // 构建缓存键
            String cacheKey = buildCacheKey(dto);

            // 尝试从缓存获取
            PagePublicArticleResponseDTO cached = publicArticleListCache.getIfPresent(cacheKey);
            if (cached != null) {
                log.debug("从缓存获取公共文章列表，key={}", cacheKey);
                return SaResult.data(cached);
            }

            // 查询所有分类用于后续映射
            Map<Long, String> categoryMap = getCategoryMap();

            // 构建查询条件 - 只查询公开的文章（is_hidden = 0）
            LambdaQueryWrapper<SysBlog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysBlog::getHidden, false)
                    .orderByDesc(SysBlog::getTop)
                    .orderByDesc(SysBlog::getCreateTime);

            // 如果传入了分类ID，只查询该分类下的文章
            Long categoryId = dto.getCategoryId();
            if (categoryId != null) {
                queryWrapper.eq(SysBlog::getCategoryId, categoryId);
            }

            // 关键词搜索：同时搜索标题、摘要、分类名称和标签
            // 如果传入了分类ID，搜索范围限制在该分类内
            if (StringUtils.hasText(dto.getKeyword())) {
                String keyword = dto.getKeyword().trim();
                
                if (categoryId != null) {
                    // 如果传入了分类ID，只在该分类内搜索（标题、摘要、标签）
                    queryWrapper.and(w -> w.like(SysBlog::getTitle, keyword)
                            .or().like(SysBlog::getSummary, keyword)
                            .or().like(SysBlog::getTags, keyword));
                } else {
                    // 如果没有传入分类ID，搜索所有分类
                    // 先查询分类名称包含关键词的分类ID
                    List<SysCategory> matchedCategories = categoryMapper.selectList(
                            new LambdaQueryWrapper<SysCategory>()
                                    .like(SysCategory::getName, keyword)
                                    .eq(SysCategory::getHidden, false)
                    );
                    List<Long> matchedCategoryIds = matchedCategories.stream()
                            .map(SysCategory::getId)
                            .collect(Collectors.toList());

                    // 筛选条件：标题/摘要匹配关键词 OR 分类匹配 OR 标签匹配
                    queryWrapper.and(w -> {
                        // 标题或摘要包含关键词
                        w.like(SysBlog::getTitle, keyword)
                                .or().like(SysBlog::getSummary, keyword);
                        // 或者分类名称匹配（通过分类ID关联）
                        if (!matchedCategoryIds.isEmpty()) {
                            w.or().in(SysBlog::getCategoryId, matchedCategoryIds);
                        }
                        // 或者标签匹配
                        w.or().like(SysBlog::getTags, keyword);
                    });
                }
            }

            // 分页查询
            Page<SysBlog> page = new Page<>(dto.getCurrentPage(), dto.getPageSize());
            Page<SysBlog> pageResult = blogMapper.selectPage(page, queryWrapper);

            // 收集所有作者 ID
            List<Long> authorIds = pageResult.getRecords().stream()
                    .map(SysBlog::getAuthorId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            // 批量查询用户信息
            Map<Long, String> authorNicknameMap = new HashMap<>();
            if (!authorIds.isEmpty()) {
                List<SysUser> users = userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>().in(SysUser::getId, authorIds)
                );
                for (SysUser user : users) {
                    authorNicknameMap.put(user.getId(), user.getNickname());
                }
            }

            // 转换为响应DTO
            List<PublicArticleResponseDTO> records = pageResult.getRecords().stream()
                    .map(blog -> convertToResponseDTO(blog, categoryMap, authorNicknameMap))
                    .collect(Collectors.toList());

            // 构建响应
            PagePublicArticleResponseDTO response = new PagePublicArticleResponseDTO();
            response.setRecords(records);
            response.setTotal(pageResult.getTotal());
            response.setSize(pageResult.getSize());
            response.setCurrent(pageResult.getCurrent());
            response.setPages(pageResult.getPages());

            // 存入缓存
            publicArticleListCache.put(cacheKey, response);

            return SaResult.data(response);
        } catch (Exception e) {
            log.error("分页获取公共文章列表异常", e);
            return SaResult.error("获取文章列表失败").setCode(500);
        }
    }

    /**
     * 将实体转换为公共文章响应DTO
     */
    private PublicArticleResponseDTO convertToResponseDTO(SysBlog blog,
                                                          Map<Long, String> categoryMap,
                                                          Map<Long, String> authorNicknameMap) {
        PublicArticleResponseDTO dto = new PublicArticleResponseDTO();
        dto.setId(blog.getId());
        dto.setCategoryId(blog.getCategoryId());
        dto.setCategoryName(categoryMap.get(blog.getCategoryId()));
        dto.setTitle(blog.getTitle());
        // 处理摘要：如果为空则从HTML内容中提取
        dto.setSummary(getSummary(blog));
        dto.setCoverImage(blog.getCoverImage());
        dto.setTags(blog.getTags());
        dto.setViewCount(blog.getViewCount());
        dto.setCommentCount(blog.getCommentCount());
        dto.setLikeCount(blog.getLikeCount());
        dto.setIsTop(blog.getTop());
        // 作者昵称
        String nickname = authorNicknameMap.get(blog.getAuthorId());
        dto.setAuthorNickname(nickname != null ? nickname : "未知作者");
        dto.setCreateTime(blog.getCreateTime());
        return dto;
    }

    /**
     * 获取摘要：如果为空则从MD内容中提取纯文本
     */
    private String getSummary(SysBlog blog) {
        if (StringUtils.hasText(blog.getSummary())) {
            return blog.getSummary();
        }
        // 从MD内容中提取纯文本并截取100个字
        if (StringUtils.hasText(blog.getContent())) {
            String plainText = MarkdownUtil.stripMdTags(blog.getContent());
            if (plainText.length() > 100) {
                return plainText.substring(0, 100) + "...";
            }
            return plainText;
        }
        return null;
    }

    /**
     * 获取分类ID到名称的映射
     */
    private Map<Long, String> getCategoryMap() {
        Map<Long, String> categoryMap = new HashMap<>();
        List<SysCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<SysCategory>()
                        .eq(SysCategory::getHidden, false)
        );
        for (SysCategory category : categories) {
            categoryMap.put(category.getId(), category.getName());
        }
        return categoryMap;
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(PagePublicArticleDTO dto) {
        return CacheUtil.CACHE_KEY_PUBLIC_ARTICLE_LIST +
                dto.getCurrentPage() + "-" +
                dto.getPageSize() + "-" +
                (dto.getCategoryId() != null ? dto.getCategoryId() : "") + "-" +
                (dto.getKeyword() != null ? dto.getKeyword() : "");
    }

    /**
     * 清除公共文章列表缓存
     * 当后台对文章进行增删改操作时，需要调用此方法清除缓存
     */
    public void clearPublicArticleCache() {
        publicArticleListCache.invalidateAll();
        log.debug("公共文章列表缓存已清除");
    }
}
