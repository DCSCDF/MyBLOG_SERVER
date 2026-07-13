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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jiuliu.myblog_dev.dto.blog.publicity.PagePublicArticleDTO;
import com.jiuliu.myblog_dev.dto.blog.publicity.PagePublicArticleResponseDTO;
import com.jiuliu.myblog_dev.dto.blog.publicity.PublicArticleDetailResponseDTO;
import com.jiuliu.myblog_dev.dto.blog.publicity.PublicArticleResponseDTO;
import com.jiuliu.myblog_dev.entity.blog.SysBlog;
import com.jiuliu.myblog_dev.entity.blog.category.SysCategory;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.entity.user.SysUserRole;
import com.jiuliu.myblog_dev.entity.user.role.SysRole;
import com.jiuliu.myblog_dev.mapper.blog.SysBlogMapper;
import com.jiuliu.myblog_dev.mapper.blog.category.SysCategoryMapper;
import com.jiuliu.myblog_dev.mapper.blog.comment.SysCommentMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserRoleMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRoleMapper;
import com.jiuliu.myblog_dev.utils.cache.CacheUtil;
import com.jiuliu.myblog_dev.utils.markdown.MarkdownUtil;
import com.jiuliu.myblog_dev.utils.segment.ChineseSegmentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
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
    private final SysCommentMapper commentMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    public PublicArticleServiceImpl(SysBlogMapper blogMapper,
                                    SysCategoryMapper categoryMapper,
                                    SysUserMapper userMapper,
            SysCommentMapper commentMapper,
            SysRoleMapper roleMapper,
            SysUserRoleMapper userRoleMapper) {
        this.blogMapper = blogMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
        this.commentMapper = commentMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    private final Cache<String, List<Long>> superAdminUserIdCache = CacheBuilder.newBuilder()
            .maximumSize(1)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    private List<Long> getSuperAdminUserIds() {
        String cacheKey = "super_admin_user_ids";
        List<Long> cached = superAdminUserIdCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        SysRole superAdminRole = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getSuperAdmin, true)
                        .eq(SysRole::getIsDeleted, 0));

        if (superAdminRole == null) {
            superAdminUserIdCache.put(cacheKey, Collections.emptyList());
            return Collections.emptyList();
        }

        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, superAdminRole.getId()));

        List<Long> userIds = userRoles.stream()
                .map(SysUserRole::getUserId)
                .distinct()
                .collect(Collectors.toList());

        superAdminUserIdCache.put(cacheKey, userIds);
        return userIds;
    }

    @Override
    public SaResult getPagePublicArticles(PagePublicArticleDTO dto) {
        return getPagePublicArticlesWithFilter(dto, null, null);
    }

    @Override
    public SaResult getPagePublicArticlesByAdmin(PagePublicArticleDTO dto) {
        List<Long> superAdminIds = getSuperAdminUserIds();
        return getPagePublicArticlesWithFilter(dto, superAdminIds, true);
    }

    @Override
    public SaResult getPagePublicArticlesByUser(PagePublicArticleDTO dto) {
        List<Long> superAdminIds = getSuperAdminUserIds();
        return getPagePublicArticlesWithFilter(dto, superAdminIds, false);
    }

    private SaResult getPagePublicArticlesWithFilter(PagePublicArticleDTO dto, List<Long> authorIds,
            Boolean includeOnly) {
        try {
            String filterType = includeOnly != null ? (includeOnly ? "admin" : "user") : null;
            String cacheKey = buildCacheKey(dto, filterType);

            PagePublicArticleResponseDTO cached = publicArticleListCache.getIfPresent(cacheKey);
            if (cached != null) {
                return SaResult.data(cached);
            }

            if (Boolean.TRUE.equals(includeOnly) && (authorIds == null || authorIds.isEmpty())) {
                PagePublicArticleResponseDTO emptyResponse = new PagePublicArticleResponseDTO();
                emptyResponse.setRecords(Collections.emptyList());
                emptyResponse.setTotal(0L);
                emptyResponse.setSize((long) dto.getPageSize());
                emptyResponse.setCurrent((long) dto.getCurrentPage());
                emptyResponse.setPages(0L);
                publicArticleListCache.put(cacheKey, emptyResponse);
                return SaResult.data(emptyResponse);
            }

            Map<Long, String> categoryMap = getCategoryMap();

            LambdaQueryWrapper<SysBlog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysBlog::getHidden, false);

            Long categoryId = dto.getCategoryId();
            if (categoryId != null) {
                queryWrapper.eq(SysBlog::getCategoryId, categoryId);
            }

            if (authorIds != null && !authorIds.isEmpty()) {
                if (Boolean.TRUE.equals(includeOnly)) {
                    queryWrapper.in(SysBlog::getAuthorId, authorIds);
                } else {
                    queryWrapper.notIn(SysBlog::getAuthorId, authorIds);
                }
            }

            Long authorId = null;
            if (StringUtils.hasText(dto.getUsername())) {
                SysUser user = userMapper.selectOne(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getUsername, dto.getUsername())
                                .eq(SysUser::getIsDeleted, 0));
                if (user != null) {
                    authorId = user.getId();
                    queryWrapper.eq(SysBlog::getAuthorId, authorId);
                } else {
                    PagePublicArticleResponseDTO emptyResponse = new PagePublicArticleResponseDTO();
                    emptyResponse.setRecords(Collections.emptyList());
                    emptyResponse.setTotal(0L);
                    emptyResponse.setSize((long) dto.getPageSize());
                    emptyResponse.setCurrent((long) dto.getCurrentPage());
                    emptyResponse.setPages(0L);
                    return SaResult.data(emptyResponse);
                }
            }

            if (StringUtils.hasText(dto.getKeyword())) {
                return searchArticlesWithKeyword(dto, categoryMap, authorId, cacheKey, authorIds, includeOnly);
            }

            return queryArticlesWithoutKeyword(dto, categoryMap, queryWrapper, cacheKey);
        } catch (Exception e) {
            log.error("分页获取公共文章列表异常", e);
            return SaResult.error("获取文章列表失败").setCode(500);
        }
    }

    private SaResult queryArticlesWithoutKeyword(PagePublicArticleDTO dto,
                                                 Map<Long, String> categoryMap,
                                                 LambdaQueryWrapper<SysBlog> queryWrapper,
                                                 String cacheKey) {
        queryWrapper.orderByDesc(SysBlog::getTop).orderByDesc(SysBlog::getCreateTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysBlog> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                dto.getCurrentPage(), dto.getPageSize());
        com.baomidou.mybatisplus.core.metadata.IPage<SysBlog> pageResult = blogMapper.selectPage(page, queryWrapper);

        List<SysBlog> pagedArticles = pageResult.getRecords();

        Map<Long, String> authorNicknameMap = getAuthorNicknameMap(pagedArticles);

        List<PublicArticleResponseDTO> records = pagedArticles.stream()
                .map(blog -> convertToResponseDTO(blog, categoryMap, authorNicknameMap))
                .collect(Collectors.toList());

        PagePublicArticleResponseDTO response = new PagePublicArticleResponseDTO();
        response.setRecords(records);
        response.setTotal(pageResult.getTotal());
        response.setSize(pageResult.getSize());
        response.setCurrent(pageResult.getCurrent());
        response.setPages(pageResult.getPages());

        publicArticleListCache.put(cacheKey, response);

        return SaResult.data(response);
    }

    private SaResult searchArticlesWithKeyword(PagePublicArticleDTO dto,
                                               Map<Long, String> categoryMap,
            Long authorId,
            String cacheKey,
            List<Long> filterAuthorIds,
            Boolean filterIncludeOnly) {
        String keyword = dto.getKeyword().trim();
        List<String> searchTokens = ChineseSegmentUtil.segmentKeyword(keyword);
        if (searchTokens.isEmpty()) {
            searchTokens = List.of(keyword);
        }

        final int MAX_SEARCH_CANDIDATES = 500;

        LambdaQueryWrapper<SysBlog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysBlog::getHidden, false);

        if (dto.getCategoryId() != null) {
            queryWrapper.eq(SysBlog::getCategoryId, dto.getCategoryId());
        }

        if (filterAuthorIds != null && !filterAuthorIds.isEmpty()) {
            if (Boolean.TRUE.equals(filterIncludeOnly)) {
                queryWrapper.in(SysBlog::getAuthorId, filterAuthorIds);
            } else {
                queryWrapper.notIn(SysBlog::getAuthorId, filterAuthorIds);
            }
        }

        if (authorId != null) {
            queryWrapper.eq(SysBlog::getAuthorId, authorId);
        }

        boolean hasKeywordCondition = false;
        for (String token : searchTokens) {
            queryWrapper.and(w -> w.like(SysBlog::getTitle, token)
                    .or().like(SysBlog::getSummary, token)
                    .or().like(SysBlog::getTags, token));
            hasKeywordCondition = true;
        }

        if (!hasKeywordCondition) {
            queryWrapper.orderByDesc(SysBlog::getTop).orderByDesc(SysBlog::getCreateTime);
        }

        queryWrapper.last("LIMIT " + MAX_SEARCH_CANDIDATES);

        List<SysBlog> candidateArticles = blogMapper.selectList(queryWrapper);

        boolean canSearchCategory = dto.getCategoryId() == null && authorId == null;
        if (canSearchCategory) {
            Set<Long> matchedCategoryIds = new HashSet<>();
            for (String token : searchTokens) {
                List<SysCategory> tokenMatchedCategories = categoryMapper.selectList(
                        new LambdaQueryWrapper<SysCategory>()
                                .like(SysCategory::getName, token)
                                .eq(SysCategory::getHidden, false));
                tokenMatchedCategories.forEach(cat -> matchedCategoryIds.add(cat.getId()));
            }

            if (!matchedCategoryIds.isEmpty()) {
                LambdaQueryWrapper<SysBlog> categoryQuery = new LambdaQueryWrapper<>();
                categoryQuery.eq(SysBlog::getHidden, false)
                        .in(SysBlog::getCategoryId, matchedCategoryIds);

                if (filterAuthorIds != null && !filterAuthorIds.isEmpty()) {
                    if (Boolean.TRUE.equals(filterIncludeOnly)) {
                        categoryQuery.in(SysBlog::getAuthorId, filterAuthorIds);
                    } else {
                        categoryQuery.notIn(SysBlog::getAuthorId, filterAuthorIds);
                    }
                }

                categoryQuery.last("LIMIT " + MAX_SEARCH_CANDIDATES);
                List<SysBlog> categoryMatchedArticles = blogMapper.selectList(categoryQuery);

                Set<Long> existingIds = candidateArticles.stream()
                        .map(SysBlog::getId)
                        .collect(Collectors.toSet());

                for (SysBlog article : categoryMatchedArticles) {
                    if (!existingIds.contains(article.getId()) && candidateArticles.size() < MAX_SEARCH_CANDIDATES) {
                        candidateArticles.add(article);
                        existingIds.add(article.getId());
                    }
                }
            }
        }

        final List<String> finalSearchTokens = searchTokens;
        Map<Long, Integer> articleScoreMap = new HashMap<>();
        List<SysBlog> matchedArticles = candidateArticles.stream().filter(article -> {
            int score = calculateMatchScore(article, finalSearchTokens);
            articleScoreMap.put(article.getId(), score);
            return score > 0;
        }).sorted((a, b) -> {
            int scoreCompare = articleScoreMap.get(b.getId()).compareTo(articleScoreMap.get(a.getId()));
            if (scoreCompare != 0)
                return scoreCompare;
            return b.getCreateTime().compareTo(a.getCreateTime());
        }).collect(Collectors.toList());

        int totalMatched = matchedArticles.size();
        int totalPages = (int) Math.ceil((double) totalMatched / dto.getPageSize());
        int fromIndex = (dto.getCurrentPage() - 1) * dto.getPageSize();
        int toIndex = Math.min(fromIndex + dto.getPageSize(), totalMatched);

        List<SysBlog> pagedArticles = (fromIndex < totalMatched)
                ? matchedArticles.subList(fromIndex, toIndex)
                : Collections.emptyList();

        Map<Long, String> authorNicknameMap = getAuthorNicknameMap(pagedArticles);

        List<PublicArticleResponseDTO> records = pagedArticles.stream()
                .map(blog -> convertToResponseDTO(blog, categoryMap, authorNicknameMap))
                .collect(Collectors.toList());

        PagePublicArticleResponseDTO response = new PagePublicArticleResponseDTO();
        response.setRecords(records);
        response.setTotal((long) totalMatched);
        response.setSize((long) dto.getPageSize());
        response.setCurrent((long) dto.getCurrentPage());
        response.setPages((long) totalPages);

        publicArticleListCache.put(cacheKey, response);

        return SaResult.data(response);
    }

    private Map<Long, String> getAuthorNicknameMap(List<SysBlog> articles) {
        Map<Long, String> authorNicknameMap = new HashMap<>();
        List<Long> authorIds = articles.stream()
                .map(SysBlog::getAuthorId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (!authorIds.isEmpty()) {
            List<SysUser> users = userMapper.selectList(
                    new LambdaQueryWrapper<SysUser>().in(SysUser::getId, authorIds));
            for (SysUser user : users) {
                authorNicknameMap.put(user.getId(), user.getNickname());
            }
        }
        return authorNicknameMap;
    }

    @Override
    public SaResult getPublicArticleDetail(Long articleId) {
        try {
            if (articleId == null) {
                return SaResult.error("文章ID不能为空").setCode(400);
            }

            // 查询文章，只查询公开的文章（is_hidden = false）
            SysBlog blog = blogMapper.selectOne(
                    new LambdaQueryWrapper<SysBlog>()
                            .eq(SysBlog::getId, articleId)
                            .eq(SysBlog::getHidden, false)
            );

            if (blog == null) {
                log.warn("公共文章详情获取失败：文章不存在或已隐藏，articleId={}", articleId);
                return SaResult.error("文章不存在或已下架").setCode(404);
            }

            // 获取分类名称
            String categoryName = null;
            if (blog.getCategoryId() != null) {
                SysCategory category = categoryMapper.selectById(blog.getCategoryId());
                // 只返回未隐藏的分类名称
                if (category != null && !category.getHidden()) {
                    categoryName = category.getName();
                }
            }

            // 获取作者昵称
            String authorNickname = "未知作者";
            if (blog.getAuthorId() != null) {
                SysUser user = userMapper.selectById(blog.getAuthorId());
                if (user != null) {
                    authorNickname = user.getNickname();
                }
            }

            // 构建响应DTO
            PublicArticleDetailResponseDTO dto = new PublicArticleDetailResponseDTO();
            dto.setId(blog.getId());
            dto.setCategoryId(blog.getCategoryId());
            dto.setCategoryName(categoryName);
            dto.setTitle(blog.getTitle());
            // 返回原始Markdown内容，不渲染HTML
            dto.setMdContent(blog.getContent());
            dto.setTags(blog.getTags());
            dto.setCommentCount(commentMapper.countApprovedComments(blog.getId()));
            dto.setIsTop(blog.getTop());
            dto.setAuthorNickname(authorNickname);
            dto.setCreateTime(blog.getCreateTime());

            return SaResult.data(dto);
        } catch (Exception e) {
            log.error("获取公共文章详情异常，articleId={}", articleId, e);
            return SaResult.error("获取文章详情失败").setCode(500);
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
        dto.setCommentCount(commentMapper.countApprovedComments(blog.getId()));
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
     *
     * @param dto        查询参数
     * @param filterType 过滤类型：null-全部, admin-只超级管理员, user-排除超级管理员
     */
    private String buildCacheKey(PagePublicArticleDTO dto, String filterType) {
        return CacheUtil.CACHE_KEY_PUBLIC_ARTICLE_LIST +
                dto.getCurrentPage() + "-" +
                dto.getPageSize() + "-" +
                (dto.getCategoryId() != null ? dto.getCategoryId() : "") + "-" +
                (dto.getKeyword() != null ? dto.getKeyword() : "") + "-" +
                (dto.getUsername() != null ? dto.getUsername() : "") + "-" +
                (filterType != null ? filterType : "");
    }

    /**
     * 清除公共文章列表缓存
     * 当后台对文章进行增删改操作时，需要调用此方法清除缓存
     */
    public void clearPublicArticleCache() {
        publicArticleListCache.invalidateAll();
        superAdminUserIdCache.invalidateAll();
        log.debug("公共文章列表缓存已清除");
    }

    /**
     * 计算文章与搜索词的匹配分数
     * 标题匹配：3分/词
     * 摘要匹配：2分/词
     * 标签匹配：1分/词
     *
     * @param article      文章实体
     * @param searchTokens 分词列表
     * @return 匹配分数
     */
    private int calculateMatchScore(SysBlog article, List<String> searchTokens) {
        int score = 0;
        String title = article.getTitle() != null ? article.getTitle().toLowerCase() : "";
        String summary = getSummary(article);
        summary = summary != null ? summary.toLowerCase() : "";
        String tags = article.getTags() != null ? article.getTags().toLowerCase() : "";

        for (String token : searchTokens) {
            String tokenLower = token.toLowerCase();

            // 标题匹配：3分
            if (title.contains(tokenLower)) {
                score += 3;
            }

            // 摘要匹配：2分
            if (summary.contains(tokenLower)) {
                score += 2;
            }

            // 标签匹配：1分
            if (tags.contains(tokenLower)) {
                score += 1;
            }
        }

        return score;
    }
}
