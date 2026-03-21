/*
 * [BlogServiceImpl.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/5
 */

package com.jiuliu.myblog_dev.service.blog;

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuliu.myblog_dev.dto.blog.*;
import com.jiuliu.myblog_dev.dto.common.FilterOptionItem;
import com.jiuliu.myblog_dev.entity.blog.SysBlog;
import com.jiuliu.myblog_dev.entity.blog.category.SysCategory;
import com.jiuliu.myblog_dev.mapper.blog.SysBlogMapper;
import com.jiuliu.myblog_dev.mapper.blog.category.SysCategoryMapper;
import com.jiuliu.myblog_dev.utils.markdown.MarkdownUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BlogServiceImpl implements BlogService {

    private static final Logger log = LoggerFactory.getLogger(BlogServiceImpl.class);

    private final SysBlogMapper blogMapper;
    private final SysCategoryMapper categoryMapper;
    private final PublicArticleService publicArticleService;

    public BlogServiceImpl(SysBlogMapper blogMapper,
                           SysCategoryMapper categoryMapper,
                           PublicArticleService publicArticleService) {
        this.blogMapper = blogMapper;
        this.categoryMapper = categoryMapper;
        this.publicArticleService = publicArticleService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult createBlog(BlogCreateDTO dto, Long authorId) {
        if (!StringUtils.hasText(dto.getTitle())) {
            log.warn("文章创建失败：标题为空");
            return SaResult.error("文章标题不能为空").setCode(400);
        }

        if (dto.getSummary() != null && dto.getSummary().length() > 200) {
            log.warn("文章创建失败：摘要长度超过200字符");
            return SaResult.error("文章摘要不能超过200字符").setCode(400);
        }

        if (StringUtils.hasText(dto.getCoverImage())) {
            if (isValidUrl(dto.getCoverImage())) {
                log.warn("文章创建失败：封面图URL格式无效，url={}", dto.getCoverImage());
                return SaResult.error("封面图片URL格式无效，请输入有效的http/https链接").setCode(400);
            }
        }

        // 验证分类是否存在且未隐藏
        if (dto.getCategoryId() != null) {
            SysCategory category = categoryMapper.selectById(dto.getCategoryId());
            if (category == null) {
                log.warn("文章创建失败：分类不存在，categoryId={}", dto.getCategoryId());
                return SaResult.error("分类不存在").setCode(400);
            }
            if (category.getHidden()) {
                log.warn("文章创建失败：分类已隐藏，categoryId={}", dto.getCategoryId());
                return SaResult.error("该分类已隐藏，无法选择").setCode(400);
            }
        }

        SysBlog blog = new SysBlog();
        blog.setTitle(dto.getTitle().trim());
        blog.setCategoryId(dto.getCategoryId());

        if (StringUtils.hasText(dto.getSummary())) {
            blog.setSummary(dto.getSummary().trim());
        }

        blog.setContent(dto.getContent());

        if (StringUtils.hasText(dto.getCoverImage())) {
            blog.setCoverImage(dto.getCoverImage().trim());
        }

        if (StringUtils.hasText(dto.getTags())) {
            blog.setTags(dto.getTags().trim());
        }

        blog.setAuthorId(authorId);

        blogMapper.insert(blog);

        // 清除公共文章列表缓存
        publicArticleService.clearPublicArticleCache();

        log.info("文章创建成功，id={}，标题={}，作者ID={}", blog.getId(), blog.getTitle(), authorId);

        Map<String, Object> data = new HashMap<>();
        data.put("id", blog.getId());
        data.put("message", "文章创建成功");
        return SaResult.data(data);
    }

    private boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) {
            return true;
        }
        try {
            URL u = new URL(url);
            String scheme = u.getProtocol();
            return !"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme);
        } catch (MalformedURLException e) {
            return true;
        }
    }

    @Override
    public SaResult getPageUserBlogs(PageUserBlogDTO dto, Long userId) {
        // 构建查询条件
        LambdaQueryWrapper<SysBlog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysBlog::getAuthorId, userId);

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

        // 转换为响应DTO
        List<BlogResponseDTO> records = pageResult.getRecords().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());

        // 构建响应
        PageUserBlogResponseDTO response = new PageUserBlogResponseDTO();
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
    public SaResult getBlogDetail(Long blogId, Long userId) {
        SysBlog blog = blogMapper.selectById(blogId);

        if (blog == null) {
            return SaResult.error("文章不存在").setCode(404);
        }

        // 检查是否是作者本人
        if (!blog.getAuthorId().equals(userId)) {
            log.warn("无权限访问其他用户的文章，文章ID：{}，当前用户ID：{}", blogId, userId);
            return SaResult.error("无权限访问该文章").setCode(403);
        }

        BlogDetailResponseDTO response = convertToDetailResponseDTO(blog);

        return SaResult.data(response);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult updateBlogStatus(Long blogId, BlogStatusUpdateDTO dto, Long userId) {
        SysBlog blog = blogMapper.selectById(blogId);

        if (blog == null) {

            return SaResult.error("文章不存在").setCode(404);
        }

        // 检查是否是作者本人
        if (!blog.getAuthorId().equals(userId)) {
            log.warn("无权限修改其他用户的文章，文章ID：{}，当前用户ID：{}", blogId, userId);
            return SaResult.error("无权限修改该文章").setCode(403);
        }

        // 更新状态
        if (dto.getIsHidden() != null) {
            blog.setHidden(dto.getIsHidden());
        }
//        if (dto.getIsTop() != null) {
//            blog.setTop(dto.getIsTop());
//        }
//        if (dto.getIsRecommend() != null) {
//            blog.setRecommend(dto.getIsRecommend());
//        }

        blogMapper.updateById(blog);

        // 清除公共文章列表缓存
        publicArticleService.clearPublicArticleCache();

        log.info("文章状态更新成功，文章ID：{}", blogId);

        Map<String, Object> data = new HashMap<>();
        data.put("id", blog.getId());
        data.put("message", "文章状态更新成功");
        return SaResult.data(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult updateBlogContent(Long blogId, BlogContentUpdateDTO dto, Long userId) {
        SysBlog blog = blogMapper.selectById(blogId);

        if (blog == null) {
            return SaResult.error("文章不存在").setCode(404);
        }

        // 检查是否是作者本人
        if (!blog.getAuthorId().equals(userId)) {
            log.warn("无权修改其他用户的文章，文章ID：{}，当前用户ID：{}", blogId, userId);
            return SaResult.error("无权限修改该文章").setCode(403);
        }

        // 验证封面图URL - 支持传空字符串清空
        String coverImageValueToSet = null;
        if (dto.getCoverImage() != null) {
            String v = dto.getCoverImage().trim();
            if (!v.isEmpty()) {
                if (isValidUrl(v)) {
                    log.warn("文章更新失败：封面图URL格式无效，url={}", dto.getCoverImage());
                    return SaResult.error("封面图片URL格式无效，请输入有效的http/https链接").setCode(400);
                }
                coverImageValueToSet = v;
            }
            // v为空时，coverImageValueToSet保持null，表示清空封面图
        }

        // 使用 LambdaUpdateWrapper 来更新，可以正确处理 null 值
        LambdaUpdateWrapper<SysBlog> updateWrapper = new LambdaUpdateWrapper<SysBlog>()
                .eq(SysBlog::getId, blogId);

        if (dto.getTitle() != null && StringUtils.hasText(dto.getTitle())) {
            if (dto.getTitle().length() > 200) {
                log.warn("文章更新失败：标题长度超过200字符");
                return SaResult.error("文章标题不能超过200字符").setCode(400);
            }
            updateWrapper.set(SysBlog::getTitle, dto.getTitle().trim());
        }

        if (dto.getSummary() != null) {
            if (dto.getSummary().length() > 200) {
                log.warn("文章更新失败：摘要长度超过200字符");
                return SaResult.error("文章摘要不能超过200字符").setCode(400);
            }
            updateWrapper.set(SysBlog::getSummary, dto.getSummary().trim());
        }

        // 只有当 coverImage 不为 null 时才更新（支持清空）
        if (dto.getCoverImage() != null) {
            updateWrapper.set(SysBlog::getCoverImage, coverImageValueToSet);
        }

        if (dto.getContent() != null) {
            updateWrapper.set(SysBlog::getContent, dto.getContent());
        }
        if (dto.getTags() != null) {
            updateWrapper.set(SysBlog::getTags, dto.getTags().trim());
        }

        // 验证分类是否存在且未隐藏
        if (dto.getCategoryId() != null) {
            SysCategory category = categoryMapper.selectById(dto.getCategoryId());
            if (category == null) {
                log.warn("文章更新失败：分类不存在，categoryId={}", dto.getCategoryId());
                return SaResult.error("分类不存在").setCode(400);
            }
            if (category.getHidden()) {
                log.warn("文章更新失败：分类已隐藏，categoryId={}", dto.getCategoryId());
                return SaResult.error("该分类已隐藏，无法选择").setCode(400);
            }
            updateWrapper.set(SysBlog::getCategoryId, dto.getCategoryId());
        }

        blogMapper.update(null, updateWrapper);

        // 清除公共文章列表缓存
        publicArticleService.clearPublicArticleCache();

        log.info("文章内容更新成功，文章ID：{}", blogId);

        Map<String, Object> data = new HashMap<>();
        data.put("id", blog.getId());
        data.put("message", "文章更新成功");
        return SaResult.data(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult deleteBlog(Long blogId, Long userId) {
        SysBlog blog = blogMapper.selectById(blogId);

        if (blog == null) {
            log.warn("文章不存在，文章ID：{}", blogId);
            return SaResult.error("文章不存在").setCode(404);
        }

        // 检查是否是作者本人
        if (!blog.getAuthorId().equals(userId)) {
            log.warn("无权限删除其他用户的文章，文章ID：{}，当前用户ID：{}", blogId, userId);
            return SaResult.error("无权限删除该文章").setCode(403);
        }

        // 逻辑删除
        blogMapper.deleteById(blogId);

        // 清除公共文章列表缓存
        publicArticleService.clearPublicArticleCache();

        log.info("文章删除成功，文章ID：{}", blogId);

        Map<String, Object> data = new HashMap<>();
        data.put("id", blogId);
        data.put("message", "文章删除成功");
        return SaResult.data(data);
    }

    /**
     * 将实体转换为列表响应DTO
     */
    private BlogResponseDTO convertToResponseDTO(SysBlog blog) {
        BlogResponseDTO dto = new BlogResponseDTO();
        dto.setId(blog.getId());
        dto.setCategoryId(blog.getCategoryId());
        dto.setTitle(blog.getTitle());
        // 处理摘要：如果为空则从MD内容中提取
        dto.setSummary(getSummary(blog));
        dto.setCoverImage(blog.getCoverImage());
        dto.setTags(blog.getTags());
        dto.setViewCount(blog.getViewCount());
        dto.setCommentCount(blog.getCommentCount());
        dto.setLikeCount(blog.getLikeCount());
        dto.setIsHidden(blog.getHidden());
        dto.setIsTop(blog.getTop());
        dto.setIsRecommend(blog.getRecommend());
        return dto;
    }

    /**
     * 将实体转换为详情响应DTO
     */
    private BlogDetailResponseDTO convertToDetailResponseDTO(SysBlog blog) {
        BlogDetailResponseDTO dto = new BlogDetailResponseDTO();
        dto.setId(blog.getId());
        dto.setCategoryId(blog.getCategoryId());
        dto.setTitle(blog.getTitle());
        dto.setSummary(blog.getSummary());
        dto.setContent(blog.getContent());
        dto.setCoverImage(blog.getCoverImage());
        dto.setTags(blog.getTags());
        dto.setAuthorId(blog.getAuthorId());
        dto.setViewCount(blog.getViewCount());
        dto.setCommentCount(blog.getCommentCount());
        dto.setLikeCount(blog.getLikeCount());
        dto.setIsHidden(blog.getHidden());
        dto.setIsTop(blog.getTop());
        dto.setIsRecommend(blog.getRecommend());
        dto.setCreateTime(blog.getCreateTime());
        dto.setUpdateTime(blog.getUpdateTime());
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
