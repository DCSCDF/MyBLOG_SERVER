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
import com.jiuliu.myblog_dev.mapper.blog.SysBlogMapper;
import com.jiuliu.myblog_dev.mapper.blog.category.SysCategoryMapper;
import com.jiuliu.myblog_dev.mapper.blog.comment.SysCommentMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.utils.cache.CacheUtil;
import com.jiuliu.myblog_dev.utils.html.HtmlUtil;
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

    public PublicArticleServiceImpl(SysBlogMapper blogMapper,
                                    SysCategoryMapper categoryMapper,
                                    SysUserMapper userMapper,
                                    SysCommentMapper commentMapper) {
        this.blogMapper = blogMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
        this.commentMapper = commentMapper;
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

            // 构建查询条件 - 只查询公开的文章
            LambdaQueryWrapper<SysBlog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysBlog::getHidden, false);

            // 如果传入了分类ID，只查询该分类下的文章
            Long categoryId = dto.getCategoryId();
            if (categoryId != null) {
                queryWrapper.eq(SysBlog::getCategoryId, categoryId);
            }

            // 关键词搜索：使用分词器进行智能分词搜索
            // 例如：搜索 "登陆 授权" 可以匹配到 "登录接口返回的一次性授权码"
            String keyword = null;
            List<String> searchTokens = Collections.emptyList();
            if (StringUtils.hasText(dto.getKeyword())) {
                keyword = dto.getKeyword().trim();
                log.info("【文章搜索】接收关键词 keyword={}", keyword);

                // 对关键词进行分词，获取分词列表
                searchTokens = ChineseSegmentUtil.segmentKeyword(keyword);
                log.info("【文章搜索】关键词分词结果 keyword={}, searchTokens={}", keyword, searchTokens);

                if (searchTokens.isEmpty()) {
                    // 分词为空时，用原始关键词搜索
                    searchTokens = List.of(keyword);
                }

                // 创建 final 副本供 lambda 使用
                final List<String> finalSearchTokens = searchTokens;

                if (categoryId != null) {
                    // 如果传入了分类ID，只在该分类内搜索
                    queryWrapper.and(w -> {
                        for (String token : finalSearchTokens) {
                            w.or().like(SysBlog::getTitle, token)
                                    .or().like(SysBlog::getSummary, token)
                                    .or().like(SysBlog::getTags, token);
                        }
                    });
                } else {
                    // 如果没有传入分类ID，搜索所有分类
                    // 先查询分类名称匹配的分词ID
                    List<SysCategory> matchedCategories = categoryMapper.selectList(
                            new LambdaQueryWrapper<SysCategory>()
                                    .like(SysCategory::getName, keyword)
                                    .eq(SysCategory::getHidden, false)
                    );
                    List<Long> matchedCategoryIds = matchedCategories.stream()
                            .map(SysCategory::getId)
                            .collect(Collectors.toList());

                    // 同时对分词后的词进行分类匹配
                    for (String token : searchTokens) {
                        List<SysCategory> tokenMatchedCategories = categoryMapper.selectList(
                                new LambdaQueryWrapper<SysCategory>()
                                        .like(SysCategory::getName, token)
                                        .eq(SysCategory::getHidden, false)
                        );
                        for (SysCategory cat : tokenMatchedCategories) {
                            if (!matchedCategoryIds.contains(cat.getId())) {
                                matchedCategoryIds.add(cat.getId());
                            }
                        }
                    }

                    // 创建 final 副本供 lambda 使用
                    final List<Long> finalMatchedCategoryIds = matchedCategoryIds;

                    // 筛选条件：标题/摘要/标签匹配任意分词词 OR 分类匹配
                    queryWrapper.and(w -> {
                        for (String token : finalSearchTokens) {
                            w.or().like(SysBlog::getTitle, token)
                                    .or().like(SysBlog::getSummary, token)
                                    .or().like(SysBlog::getTags, token);
                        }
                        // 分类匹配
                        if (!finalMatchedCategoryIds.isEmpty()) {
                            w.or().in(SysBlog::getCategoryId, finalMatchedCategoryIds);
                        }
                    });
                }
            }

            // 排序规则：置顶优先，然后按匹配分数，最后按创建时间
            // 注意：先不加排序，拿到所有匹配文章后在 Java 中计算匹配分数排序
            queryWrapper.orderByDesc(SysBlog::getTop)
                    .orderByDesc(SysBlog::getCreateTime);

            // 先查询所有匹配的文章（不限制数量，用于计算匹配分数）
            List<SysBlog> allMatchedArticles = blogMapper.selectList(queryWrapper);

            // 计算每篇文章的匹配分数：标题匹配3分，摘要匹配2分，标签匹配1分
            final List<String> finalSearchTokens = searchTokens;
            Map<Long, Integer> articleScoreMap = new HashMap<>();
            for (SysBlog article : allMatchedArticles) {
                int score = calculateMatchScore(article, finalSearchTokens);
                articleScoreMap.put(article.getId(), score);
            }

            // 按匹配分数降序排序，分数相同时按创建时间降序
            allMatchedArticles.sort((a, b) -> {
                int scoreCompare = articleScoreMap.get(b.getId()).compareTo(articleScoreMap.get(a.getId()));
                if (scoreCompare != 0) return scoreCompare;
                // 分数相同按创建时间降序（新的在前）
                return b.getCreateTime().compareTo(a.getCreateTime());
            });

            // 计算总匹配数（用于日志）
            int totalMatched = allMatchedArticles.size();
            int totalPages = (int) Math.ceil((double) totalMatched / dto.getPageSize());
            int fromIndex = (dto.getCurrentPage() - 1) * dto.getPageSize();
            int toIndex = Math.min(fromIndex + dto.getPageSize(), totalMatched);

            // 分页截取
            List<SysBlog> pagedArticles = (fromIndex < totalMatched)
                    ? allMatchedArticles.subList(fromIndex, toIndex)
                    : Collections.emptyList();

            // 收集所有作者 ID
            List<Long> authorIds = pagedArticles.stream()
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
            List<PublicArticleResponseDTO> records = pagedArticles.stream()
                    .map(blog -> convertToResponseDTO(blog, categoryMap, authorNicknameMap))
                    .collect(Collectors.toList());

            // 打印搜索结果日志
            log.info("【文章搜索结果】keyword={}, searchTokens={}, totalMatched={}, pageTotal={}, results=[{}]",
                    keyword, searchTokens, totalMatched, totalPages,
                    pagedArticles.stream()
                            .map(b -> b.getTitle() + "(score:" + articleScoreMap.get(b.getId()) + ")")
                            .collect(Collectors.joining(", ")));

            // 构建响应
            PagePublicArticleResponseDTO response = new PagePublicArticleResponseDTO();
            response.setRecords(records);
            response.setTotal((long) totalMatched);
            response.setSize((long) dto.getPageSize());
            response.setCurrent((long) dto.getCurrentPage());
            response.setPages((long) totalPages);

            // 存入缓存
            publicArticleListCache.put(cacheKey, response);

            return SaResult.data(response);
        } catch (Exception e) {
            log.error("分页获取公共文章列表异常", e);
            return SaResult.error("获取文章列表失败").setCode(500);
        }
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
            // MD转HTML并净化XSS
            dto.setHtmlContent(HtmlUtil.markdownToHtml(blog.getContent()));
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
