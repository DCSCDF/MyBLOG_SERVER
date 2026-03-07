/*
 * [GlobalArticleServiceImpl.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/8
 */

package com.jiuliu.myblog_dev.service.blog;

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jiuliu.myblog_dev.dto.blog.global.*;
import com.jiuliu.myblog_dev.dto.common.FilterOptionItem;
import com.jiuliu.myblog_dev.entity.blog.SysBlog;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.mapper.blog.SysBlogMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 全局文章管理 Service 实现类
 */
@Service
public class GlobalArticleServiceImpl implements GlobalArticleService {

    private static final Logger log = LoggerFactory.getLogger(GlobalArticleServiceImpl.class);

    /**
     * 文章缓存 - 缓存单个文章，key为文章ID，value为SysBlog对象
     * 缓存时间：30分钟
     */
    private final Cache<Long, SysBlog> articleCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    /**
     * 文章列表缓存 - 缓存文章列表
     * 缓存时间：30分钟
     */
    private final Cache<String, List<SysBlog>> articleListCache = CacheBuilder.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();


    private final SysBlogMapper blogMapper;
    private final SysUserMapper userMapper;

    public GlobalArticleServiceImpl(SysBlogMapper blogMapper, SysUserMapper userMapper) {
        this.blogMapper = blogMapper;
        this.userMapper = userMapper;
    }

    @Override
    public SaResult getPageGlobalArticles(PageGlobalArticleDTO dto) {
        // 构建查询条件
        LambdaQueryWrapper<SysBlog> queryWrapper = new LambdaQueryWrapper<>();

        // 关键词搜索（标题）
        if (StringUtils.hasText(dto.getKeyword())) {
            queryWrapper.like(SysBlog::getTitle, dto.getKeyword());
        }

        // 状态筛选
        if (dto.getIsHidden() != null) {
            queryWrapper.eq(SysBlog::getHidden, dto.getIsHidden());
        }
        if (dto.getIsTop() != null) {
            queryWrapper.eq(SysBlog::getTop, dto.getIsTop());
        }
        if (dto.getIsRecommend() != null) {
            queryWrapper.eq(SysBlog::getRecommend, dto.getIsRecommend());
        }

        // 按置顶和创建时间排序
        queryWrapper.orderByDesc(SysBlog::getTop, SysBlog::getCreateTime);

        // 分页查询
        Page<SysBlog> page = new Page<>(dto.getCurrentPage(), dto.getPageSize());
        IPage<SysBlog> pageResult = blogMapper.selectPage(page, queryWrapper);

        // 收集所有作者 ID
        List<Long> authorIds = pageResult.getRecords().stream()
                .map(SysBlog::getAuthorId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询用户信息
        Map<Long, UserInfo> userInfoMap = new HashMap<>();
        if (!authorIds.isEmpty()) {
            List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, authorIds));
            for (SysUser user : users) {
                userInfoMap.put(user.getId(), new UserInfo(user.getNickname(), user.getStatus(), user.getIsDeleted()));
            }
        }

        // 转换为响应DTO
        List<GlobalArticleResponseDTO> records = pageResult.getRecords().stream()
                .map(blog -> convertToGlobalArticleResponseDTO(blog, userInfoMap))
                .collect(Collectors.toList());

        // 构建响应
        PageGlobalArticleResponseDTO response = new PageGlobalArticleResponseDTO();
        response.setRecords(records);
        response.setTotal(pageResult.getTotal());
        response.setSize(pageResult.getSize());
        response.setCurrent(pageResult.getCurrent());
        response.setPages(pageResult.getPages());

        // 设置筛选项
        response.setFilterOptions(buildFilterOptions());

        return SaResult.data(response);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult updateArticleStatus(Long blogId, GlobalArticleStatusUpdateDTO dto) {
        SysBlog blog = blogMapper.selectById(blogId);

        if (blog == null) {
            return SaResult.error("文章不存在").setCode(404);
        }

        // 更新状态
        if (dto.getIsHidden() != null) {
            blog.setHidden(dto.getIsHidden());
        }
        if (dto.getIsTop() != null) {
            blog.setTop(dto.getIsTop());
        }
        if (dto.getIsRecommend() != null) {
            blog.setRecommend(dto.getIsRecommend());
        }

        blogMapper.updateById(blog);

        // 清除文章缓存
        clearArticleCache(blogId);

        log.info("文章状态更新成功，文章ID：{}", blogId);

        Map<String, Object> data = new HashMap<>();
        data.put("id", blog.getId());
        data.put("message", "文章状态更新成功");
        return SaResult.data(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult deleteArticle(Long blogId) {
        SysBlog blog = blogMapper.selectById(blogId);

        if (blog == null) {
            return SaResult.error("文章不存在").setCode(404);
        }

        // 逻辑删除
        blogMapper.deleteById(blogId);

        // 清除文章缓存
        clearArticleCache(blogId);

        log.info("文章删除成功，文章ID：{}", blogId);

        Map<String, Object> data = new HashMap<>();
        data.put("id", blogId);
        data.put("message", "文章删除成功");
        return SaResult.data(data);
    }

    /**
     * 清除文章缓存
     *
     * @param blogId 文章ID
     */
    private void clearArticleCache(Long blogId) {
        articleCache.invalidate(blogId);
        articleListCache.invalidateAll();
        log.debug("文章缓存已清除，blogId={}", blogId);
    }

    /**
     * 将实体转换为全局文章响应DTO
     */
    private GlobalArticleResponseDTO convertToGlobalArticleResponseDTO(SysBlog blog, Map<Long, UserInfo> userInfoMap) {
        GlobalArticleResponseDTO dto = new GlobalArticleResponseDTO();
        dto.setId(blog.getId());
        dto.setCategoryId(blog.getCategoryId());
        dto.setTitle(blog.getTitle());
        // 处理摘要：如果为空则从HTML内容中提取
        dto.setSummary(getSummary(blog));
        dto.setCoverImage(blog.getCoverImage());
        dto.setTags(blog.getTags());
        dto.setViewCount(blog.getViewCount());
        dto.setCommentCount(blog.getCommentCount());
        dto.setLikeCount(blog.getLikeCount());
        dto.setIsHidden(blog.getHidden());
        dto.setIsTop(blog.getTop());
        dto.setIsRecommend(blog.getRecommend());
        // 处理作者信息
        dto.setAuthorId(blog.getAuthorId());
        UserInfo userInfo = userInfoMap.get(blog.getAuthorId());
        String nickname;
        if (userInfo == null) {
            nickname = "用户已注销";
        } else {
            nickname = userInfo.getNickname();
            // 如果用户被禁用或删除，添加相应标记
            if (userInfo.getIsDeleted() != null && userInfo.getIsDeleted() == 1) {
                nickname += "[已注销]";
            } else if (userInfo.getStatus() != null && userInfo.getStatus() == 0) {
                nickname += "[已禁用]";
            }
        }
        dto.setAuthorNickname(nickname);
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
     * 构建筛选项
     */
    private Map<String, List<FilterOptionItem>> buildFilterOptions() {
        Map<String, List<FilterOptionItem>> filterOptions = new HashMap<>();

        // 隐藏状态筛选项
        List<FilterOptionItem> hiddenOptions = new ArrayList<>();
//        hiddenOptions.add(new FilterOptionItem(null, "全部"));
        hiddenOptions.add(new FilterOptionItem(false, "显示"));
        hiddenOptions.add(new FilterOptionItem(true, "隐藏"));
        filterOptions.put("isHidden", hiddenOptions);

        // 置顶状态筛选项
        List<FilterOptionItem> topOptions = new ArrayList<>();
//        topOptions.add(new FilterOptionItem(null, "全部"));
        topOptions.add(new FilterOptionItem(false, "不置顶"));
        topOptions.add(new FilterOptionItem(true, "置顶"));
        filterOptions.put("isTop", topOptions);

        // 推荐状态筛选项
        List<FilterOptionItem> recommendOptions = new ArrayList<>();
//        recommendOptions.add(new FilterOptionItem(null, "全部"));
        recommendOptions.add(new FilterOptionItem(false, "不推荐"));
        recommendOptions.add(new FilterOptionItem(true, "推荐"));
        filterOptions.put("isRecommend", recommendOptions);

        return filterOptions;
    }
}
