/*
 * [RssFeedServiceImpl.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/29
 */

package com.jiuliu.myblog_dev.service.rss;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuliu.myblog_dev.dto.rss.RssFeedResponseDTO;
import com.jiuliu.myblog_dev.entity.blog.SysBlog;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.mapper.blog.SysBlogMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.service.config.SysConfigService;
import com.jiuliu.myblog_dev.utils.html.HtmlUtil;
import com.jiuliu.myblog_dev.utils.markdown.MarkdownUtil;
import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.SyndFeedOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.StringWriter;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RSS Feed Service 实现类
 */
@Service
public class RssFeedServiceImpl implements RssFeedService {

    private static final Logger log = LoggerFactory.getLogger(RssFeedServiceImpl.class);

    private static final int DEFAULT_ARTICLE_LIMIT = 10;
    private static final String FEED_TYPE = "atom_1.0";

    private final SysBlogMapper blogMapper;
    private final SysUserMapper userMapper;
    private final SysConfigService sysConfigService;

    public RssFeedServiceImpl(SysBlogMapper blogMapper,
                              SysUserMapper userMapper,
                              SysConfigService sysConfigService) {
        this.blogMapper = blogMapper;
        this.userMapper = userMapper;
        this.sysConfigService = sysConfigService;
    }

    @Override
    public RssFeedResponseDTO generateRssFeed() {
        RssFeedResponseDTO response = new RssFeedResponseDTO();

        // 获取网站配置
        Map<String, String> siteConfig = getSiteConfig();

        // 查询最新10篇公开文章（按创建时间倒序）
        List<SysBlog> articles = getLatestArticles();

        // 收集作者ID并批量查询
        Set<Long> authorIds = articles.stream()
                .map(SysBlog::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> authorMap = new HashMap<>();
        if (!authorIds.isEmpty()) {
            List<SysUser> authors = userMapper.selectList(
                    new LambdaQueryWrapper<SysUser>().in(SysUser::getId, authorIds));
            for (SysUser user : authors) {
                authorMap.put(user.getId(), user.getNickname());
            }
        }

        // 构建站点基础URL
        String siteUrl = siteConfig.getOrDefault("site.domain", "https://example.com");
        if (!siteUrl.startsWith("http")) {
            siteUrl = "https://" + siteUrl;
        }
        // 确保URL不以/结尾
        if (siteUrl.endsWith("/")) {
            siteUrl = siteUrl.substring(0, siteUrl.length() - 1);
        }

        // 生成 Atom Feed
        String feedXml = generateAtomFeed(siteConfig, articles, authorMap, siteUrl);

        // 构建响应
        response.setFeedXml(feedXml);
        response.setArticleCount(articles.size());
        response.setArticleTitles(articles.stream()
                .map(SysBlog::getTitle)
                .collect(Collectors.toList()));

        log.info("RSS Feed生成成功，共 {} 篇文章", articles.size());
        return response;
    }

    /**
     * 生成 Atom Feed XML
     */
    private String generateAtomFeed(Map<String, String> siteConfig,
                                    List<SysBlog> articles,
                                    Map<Long, String> authorMap,
                                    String siteUrl) {
        // 使用 SyndFeed 创建 Atom 1.0 Feed
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType(FEED_TYPE);

        // 设置 Feed 元数据
        feed.setTitle(siteConfig.getOrDefault("site.name", "My Blog"));
        feed.setDescription(siteConfig.getOrDefault("site.description", "RSS Feed"));
        feed.setAuthor(siteConfig.getOrDefault("site.name", "My Blog"));

        // Feed 链接
        feed.setLink(siteUrl);
        feed.setUri(siteUrl + "/rss");

        // 生成时间
        feed.setPublishedDate(new Date());

        // 生成文章条目
        List<SyndEntry> entries = new ArrayList<>();
        for (SysBlog article : articles) {
            SyndEntry entry = new SyndEntryImpl();
            entry.setTitle(article.getTitle());

            // 文章链接
            String articleUrl = siteUrl + "/article/" + article.getId();
            entry.setLink(articleUrl);
            entry.setUri("urn:uuid:" + UUID.nameUUIDFromBytes(articleUrl.getBytes()));

            // 发布时间
            if (article.getCreateTime() != null) {
                entry.setPublishedDate(Date.from(article.getCreateTime()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()));
            } else {
                entry.setPublishedDate(new Date());
            }

            // 作者信息
            if (article.getAuthorId() != null && authorMap.containsKey(article.getAuthorId())) {
                entry.setAuthor(authorMap.get(article.getAuthorId()));
            }

            // 文章摘要（用于 Feed 阅读器预览）
            String summary = buildArticleSummary(article);
            SyndContent description = new SyndContentImpl();
            description.setType("html");
            description.setValue(summary);
            entry.setDescription(description);

            // 文章完整内容（HTML格式）
            String fullContent = buildFullContent(article, siteUrl);
            SyndContent content = new SyndContentImpl();
            content.setType("html");
            content.setValue(fullContent);
            entry.setContents(List.of(content));

            // 标签
            if (article.getTags() != null && !article.getTags().isBlank()) {
                List<com.rometools.rome.feed.synd.SyndCategory> categories = Arrays.stream(
                                article.getTags().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(tag -> {
                            com.rometools.rome.feed.synd.SyndCategory cat = new com.rometools.rome.feed.synd.SyndCategoryImpl();
                            cat.setName(tag);
                            return cat;
                        })
                        .collect(Collectors.toList());
                entry.setCategories(categories);
            }

            entries.add(entry);
        }

        feed.setEntries(entries);

        // 转换为 XML 字符串
        return feedToXml(feed);
    }

    /**
     * 将 Feed 对象转换为 XML 字符串
     */
    private String feedToXml(SyndFeed feed) {
        try {
            StringWriter writer = new StringWriter();
            SyndFeedOutput output = new SyndFeedOutput();
            output.output(feed, writer);
            return writer.toString();
        } catch (Exception e) {
            log.error("RSS Feed XML生成失败", e);
            throw new RuntimeException("RSS Feed XML生成失败", e);
        }
    }

    /**
     * 构建文章摘要
     */
    @SuppressWarnings("null")
    private String buildArticleSummary(SysBlog article) {
        StringBuilder summary = new StringBuilder();

        // 优先使用摘要字段
        if (article.getSummary() != null && !article.getSummary().isBlank()) {
            summary.append(article.getSummary());
        }

        // 如果没有摘要，从内容中提取
        if (summary.isEmpty() && article.getContent() != null) {
            String plainText = MarkdownUtil.stripMdTags(article.getContent());
            summary.append(plainText);
        }

        // 转义HTML特殊字符
        return HtmlUtils.htmlEscape(summary.toString());
    }

    /**
     * 构建文章完整内容（HTML格式）
     */
    private String buildFullContent(SysBlog article, String siteUrl) {
        StringBuilder content = new StringBuilder();

        // 构建文章元信息
        content.append("<div style='margin-bottom: 20px; color: #666;'>");
        if (article.getAuthorId() != null) {
            String authorName = getAuthorName(article.getAuthorId());
            content.append("<span>作者：").append(HtmlUtils.htmlEscape(authorName != null ? authorName : ""))
                    .append("</span>");
        }
        if (article.getCreateTime() != null) {
            content.append(" &nbsp;|&nbsp; ");
            content.append("<span>发布时间：").append(article.getCreateTime().toString()).append("</span>");
        }
        if (article.getTags() != null && !article.getTags().isBlank()) {
            content.append(" &nbsp;|&nbsp; ");
            String tags = article.getTags();
            content.append("<span>标签：").append(HtmlUtils.htmlEscape(tags != null ? tags : "")).append("</span>");
        }
        content.append("</div>");

        // 将 Markdown 内容转换为 HTML
        if (article.getContent() != null) {
            String htmlContent = HtmlUtil.markdownToHtml(article.getContent());
            content.append(htmlContent);
        }

        // 添加原文链接
        String articleUrl = siteUrl + "/article/" + article.getId();
        content.append("<hr/><p style='color: #888; font-size: 12px;'>");
        content.append("原文链接：<a href='").append(articleUrl).append("'>").append(articleUrl).append("</a>");
        content.append("</p>");

        return content.toString();
    }

    /**
     * 获取作者名称（简单缓存）
     */
    private String getAuthorName(Long authorId) {
        if (authorId == null) {
            return "未知作者";
        }
        try {
            var user = userMapper.selectById(authorId);
            return user != null ? user.getNickname() : "未知作者";
        } catch (Exception e) {
            return "未知作者";
        }
    }

    /**
     * 获取最新公开文章列表
     */
    private List<SysBlog> getLatestArticles() {
        LambdaQueryWrapper<SysBlog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysBlog::getHidden, false)
                .orderByDesc(SysBlog::getCreateTime)
                .last("LIMIT " + RssFeedServiceImpl.DEFAULT_ARTICLE_LIMIT);

        return blogMapper.selectList(queryWrapper);
    }

    /**
     * 获取网站配置
     */
    private Map<String, String> getSiteConfig() {
        Map<String, String> config = new HashMap<>();
        try {
            var result = sysConfigService.getSiteInfo();
            if (result.getCode() == 200 && result.getData() != null) {
                com.jiuliu.myblog_dev.dto.config.SiteInfoDTO siteInfo = (com.jiuliu.myblog_dev.dto.config.SiteInfoDTO) result
                        .getData();
                if (siteInfo.getSiteName() != null) {
                    config.put("site.name", siteInfo.getSiteName());
                }
                if (siteInfo.getSiteDomain() != null) {
                    config.put("site.domain", siteInfo.getSiteDomain());
                }
                if (siteInfo.getSiteDescription() != null) {
                    config.put("site.description", siteInfo.getSiteDescription());
                }
            }
        } catch (Exception e) {
            log.warn("获取网站配置失败，使用默认值", e);
        }
        return config;
    }
}
