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
import com.jiuliu.myblog_dev.dto.common.FilterOptionItem;
import com.jiuliu.myblog_dev.entity.blog.SysBlog;
import com.jiuliu.myblog_dev.entity.blog.category.SysCategory;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.mapper.blog.SysBlogMapper;
import com.jiuliu.myblog_dev.mapper.blog.category.SysCategoryMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.utils.cache.CacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
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

            // 关键词搜索：同时搜索标题和摘要
            if (StringUtils.hasText(dto.getKeyword())) {
                String keyword = dto.getKeyword().trim();
                queryWrapper.and(w -> w.like(SysBlog::getTitle, keyword)
                        .or().like(SysBlog::getSummary, keyword));
            }

            // 分类筛选 - 模糊搜索
            if (StringUtils.hasText(dto.getCategoryName())) {
                String categoryName = dto.getCategoryName().trim();
                List<SysCategory> matchedCategories = categoryMapper.selectList(
                        new LambdaQueryWrapper<SysCategory>()
                                .like(SysCategory::getName, categoryName)
                                .eq(SysCategory::getHidden, false)
                );
                if (!matchedCategories.isEmpty()) {
                    List<Long> categoryIds = matchedCategories.stream()
                            .map(SysCategory::getId)
                            .collect(Collectors.toList());
                    queryWrapper.in(SysBlog::getCategoryId, categoryIds);
                } else {
                    // 没有匹配的分类，返回空结果
                    PagePublicArticleResponseDTO emptyResponse = createEmptyResponse();
                    publicArticleListCache.put(cacheKey, emptyResponse);
                    return SaResult.data(emptyResponse);
                }
            }

            // 标签筛选 - 模糊搜索
            if (StringUtils.hasText(dto.getTagName())) {
                String tagName = dto.getTagName().trim();
                // 使用LIKE进行模糊匹配标签字段
                queryWrapper.like(SysBlog::getTags, tagName);
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
            response.setCategoryOptions(buildCategoryOptions(categoryMap));
            response.setTagOptions(buildTagOptions());

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
     * 获取摘要：如果为空则从HTML内容中提取
     */
    private String getSummary(SysBlog blog) {
        if (StringUtils.hasText(blog.getSummary())) {
            return blog.getSummary();
        }
        // 从HTML内容中提取纯文本并截取50字
        if (StringUtils.hasText(blog.getHtmlContent())) {
            String plainText = stripHtmlTags(blog.getHtmlContent());
            if (plainText.length() > 50) {
                return plainText.substring(0, 50) + "...";
            }
            return plainText;
        }
        return null;
    }

    /**
     * 去除HTML标签
     */
    private String stripHtmlTags(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return "";
        }
        // 去除script和style标签及其内容
        Pattern scriptPattern = Pattern.compile("<script[^>]*>[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE);
        htmlContent = scriptPattern.matcher(htmlContent).replaceAll("");

        Pattern stylePattern = Pattern.compile("<style[^>]*>[\\s\\S]*?</style>", Pattern.CASE_INSENSITIVE);
        htmlContent = stylePattern.matcher(htmlContent).replaceAll("");

        // 去除所有HTML标签
        Pattern htmlPattern = Pattern.compile("<[^>]+>");
        htmlContent = htmlPattern.matcher(htmlContent).replaceAll("");

        // 替换HTML实体
        htmlContent = htmlContent.replaceAll("&nbsp;", " ")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();

        return htmlContent;
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
     * 构建分类筛选项
     */
    private List<FilterOptionItem> buildCategoryOptions(Map<Long, String> categoryMap) {
        return categoryMap.entrySet().stream()
                .map(entry -> new FilterOptionItem(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(FilterOptionItem::getLabel))
                .collect(Collectors.toList());
    }

    /**
     * 构建标签筛选项 - 从所有文章中提取
     */
    private List<FilterOptionItem> buildTagOptions() {
        List<String> allTags = blogMapper.selectList(
                        new LambdaQueryWrapper<SysBlog>()
                                .eq(SysBlog::getHidden, false)
                                .isNotNull(SysBlog::getTags)
                ).stream()
                .map(SysBlog::getTags)
                .filter(StringUtils::hasText)
                .toList();

        // 提取所有唯一标签
        Set<String> uniqueTags = new LinkedHashSet<>();
        for (String tags : allTags) {
            String[] tagArray = tags.split(",");
            for (String tag : tagArray) {
                String trimmedTag = tag.trim();
                if (StringUtils.hasText(trimmedTag)) {
                    uniqueTags.add(trimmedTag);
                }
            }
        }

        return uniqueTags.stream()
                .map(tag -> new FilterOptionItem(tag, tag))
                .sorted(Comparator.comparing(FilterOptionItem::getLabel))
                .collect(Collectors.toList());
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(PagePublicArticleDTO dto) {
        return CacheUtil.CACHE_KEY_PUBLIC_ARTICLE_LIST +
                dto.getCurrentPage() + "-" +
                dto.getPageSize() + "-" +
                (dto.getKeyword() != null ? dto.getKeyword() : "") + "-" +
                (dto.getCategoryName() != null ? dto.getCategoryName() : "") + "-" +
                (dto.getTagName() != null ? dto.getTagName() : "");
    }

    /**
     * 创建空响应
     */
    private PagePublicArticleResponseDTO createEmptyResponse() {
        PagePublicArticleResponseDTO response = new PagePublicArticleResponseDTO();
        response.setRecords(Collections.emptyList());
        response.setTotal(0L);
        response.setSize(10L);
        response.setCurrent(1L);
        response.setPages(0L);
        response.setCategoryOptions(Collections.emptyList());
        response.setTagOptions(Collections.emptyList());
        return response;
    }
}
