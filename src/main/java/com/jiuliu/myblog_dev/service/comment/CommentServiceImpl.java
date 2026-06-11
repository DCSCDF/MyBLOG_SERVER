/*
 * [CommentServiceImpl.java]
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

package com.jiuliu.myblog_dev.service.comment;

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jiuliu.myblog_dev.dto.comment.*;
import com.jiuliu.myblog_dev.dto.common.FilterOptionItem;
import com.jiuliu.myblog_dev.entity.blog.SysBlog;
import com.jiuliu.myblog_dev.entity.blog.comment.SysComment;
import com.jiuliu.myblog_dev.mapper.blog.SysBlogMapper;
import com.jiuliu.myblog_dev.mapper.blog.comment.SysCommentMapper;
import com.jiuliu.myblog_dev.service.blog.PublicArticleService;
import com.jiuliu.myblog_dev.utils.cache.CacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 评论服务实现类（用户自己的评论）
 */
@Service
public class CommentServiceImpl implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);

    /**
     * 评论缓存 - 缓存单个评论，key为评论ID，value为SysComment对象
     * 缓存时间：10分钟
     */
    private final Cache<Long, SysComment> commentCache = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    /**
     * 用户评论列表缓存
     * 缓存时间：5分钟
     */
    private final Cache<String, PageCommentResponseDTO> userCommentListCache = CacheBuilder.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    private final SysCommentMapper commentMapper;
    private final SysBlogMapper blogMapper;
    private final PublicArticleService publicArticleService;

    public CommentServiceImpl(SysCommentMapper commentMapper, SysBlogMapper blogMapper,
                              PublicArticleService publicArticleService) {
        this.commentMapper = commentMapper;
        this.blogMapper = blogMapper;
        this.publicArticleService = publicArticleService;
    }

    @Override
    public SaResult getPageUserComments(PageCommentDTO dto, Long userId) {
        try {
            // 构建缓存键
            String cacheKey = CacheUtil.CACHE_KEY_USER_COMMENT_LIST + userId + "-" + dto.getCurrentPage() + "-"
                    + dto.getPageSize() + "-" + dto.getStatus();

            // 尝试从缓存获取
            PageCommentResponseDTO cached = userCommentListCache.getIfPresent(cacheKey);
            if (cached != null) {
                log.debug("从缓存获取用户评论列表，key={}", cacheKey);
                return SaResult.data(cached);
            }

            LambdaQueryWrapper<SysComment> wrapper = new LambdaQueryWrapper<SysComment>()
                    .eq(SysComment::getUserId, userId)
                    .orderByDesc(SysComment::getCreateTime);

            // 状态筛选
            if (dto.getStatus() != null) {
                wrapper.eq(SysComment::getStatus, dto.getStatus());
            }

            Page<SysComment> page = new Page<>(dto.getCurrentPage(), dto.getPageSize());
            Page<SysComment> pageResult = commentMapper.selectPage(page, wrapper);

            // 转换为响应DTO
            List<CommentResponseDTO> records = pageResult.getRecords().stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());

            // 构建响应
            PageCommentResponseDTO response = new PageCommentResponseDTO();
            response.setRecords(records);
            response.setTotal(pageResult.getTotal());
            response.setSize(pageResult.getSize());
            response.setCurrent(pageResult.getCurrent());
            response.setPages(pageResult.getPages());
            response.setFilterOptions(buildStatusFilterOptions());

            // 存入缓存
            userCommentListCache.put(cacheKey, response);

            return SaResult.data(response);
        } catch (Exception e) {
            log.error("分页获取用户评论列表异常", e);
            return SaResult.error("获取评论列表失败").setCode(500);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult updateComment(CommentUpdateDTO dto, Long userId) {
        SysComment existing = commentMapper.selectById(dto.getId());
        if (existing == null) {
            log.warn("更新评论失败：评论不存在，id={}", dto.getId());
            return SaResult.error("评论不存在").setCode(404);
        }

        // 检查是否是评论作者本人
        if (existing.getUserId() == null || !existing.getUserId().equals(userId)) {
            log.warn("无权限修改其他用户的评论，评论ID：{}，当前用户ID：{}", dto.getId(), userId);
            return SaResult.error("无权限修改该评论").setCode(403);
        }

        // 如果是子评论，检查父评论链是否都通过审核
        if (existing.getParentId() != null && existing.getParentId() != 0) {
            if (!areAllParentCommentsApproved(existing)) {
                // 父评论链中有未通过的评论，子评论只能是待审核状态且无法修改内容
                log.warn("更新评论失败：父评论链中存在未通过的评论，commentId={}", dto.getId());
                return SaResult.error("父评论尚未通过审核，无法修改此回复").setCode(400);
            }
        }

        // 更新评论内容
        LambdaUpdateWrapper<SysComment> updateWrapper = new LambdaUpdateWrapper<SysComment>()
                .eq(SysComment::getId, dto.getId());

        if (dto.getContent() != null && StringUtils.hasText(dto.getContent())) {
            updateWrapper.set(SysComment::getContent, dto.getContent().trim());
        }

        if (dto.getWebsite() != null) {
            // 支持传空字符串清空网站
            String website = dto.getWebsite().trim();
            updateWrapper.set(SysComment::getWebsite, website.isEmpty() ? null : website);
        }

        updateWrapper.set(SysComment::getUpdateTime, LocalDateTime.now());

        commentMapper.update(null, updateWrapper);
        log.info("评论更新成功，id={}", dto.getId());

        // 清除缓存
        clearCommentCache(dto.getId());

        SysComment updated = commentMapper.selectById(dto.getId());
        return SaResult.data(toResponseDTO(updated));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult deleteComment(Long commentId, Long userId) {
        SysComment existing = commentMapper.selectById(commentId);
        if (existing == null) {
            log.warn("删除评论失败：评论不存在，id={}", commentId);
            return SaResult.error("评论不存在").setCode(404);
        }

        // 检查是否是评论作者本人
        if (existing.getUserId() == null || !existing.getUserId().equals(userId)) {
            log.warn("无权限删除其他用户的评论，评论ID：{}，当前用户ID：{}", commentId, userId);
            return SaResult.error("无权限删除该评论").setCode(403);
        }

        // 记录文章ID和评论状态，用于更新评论数
        Long blogId = existing.getBlogId();
        boolean wasApproved = (existing.getStatus() != null && existing.getStatus() == 1);

        // 级联删除：先删除所有子评论（递归）
        deleteChildComments(commentId, blogId);

        // 逻辑删除：设置 is_deleted = 1
        commentMapper.update(null, new LambdaUpdateWrapper<SysComment>()
                .eq(SysComment::getId, commentId)
                .set(SysComment::getIsDeleted, 1)
                .set(SysComment::getUpdateTime, LocalDateTime.now()));

        log.info("评论删除成功，id={}", commentId);

        // 如果删除的是已通过的评论，更新文章的评论数
        if (wasApproved && blogId != null) {
            blogMapper.update(null,
                    new LambdaUpdateWrapper<SysBlog>()
                            .eq(SysBlog::getId, blogId)
                            .setSql("comment_count = GREATEST(comment_count - 1, 0)"));
            log.info("文章评论数已减少（用户删除评论），blogId={}", blogId);
        }

        // 清除公共文章缓存
        publicArticleService.clearPublicArticleCache();

        // 清除用户评论列表缓存
        clearCommentCache(commentId);

        return SaResult.data("删除成功");
    }

    /**
     * 递归删除子评论
     */
    private void deleteChildComments(Long parentId, Long blogId) {
        // 查询所有直接子评论
        List<SysComment> childComments = commentMapper.selectList(
                new LambdaQueryWrapper<SysComment>()
                        .eq(SysComment::getParentId, parentId));

        for (SysComment child : childComments) {
            // 递归删除子评论的子评论
            deleteChildComments(child.getId(), blogId);
            // 逻辑删除子评论
            commentMapper.update(null, new LambdaUpdateWrapper<SysComment>()
                    .eq(SysComment::getId, child.getId())
                    .set(SysComment::getIsDeleted, 1)
                    .set(SysComment::getUpdateTime, LocalDateTime.now()));
            clearCommentCache(child.getId());
            log.info("子评论级联删除成功，id={}", child.getId());

            // 如果子评论是已通过的，也更新文章评论数
            if (child.getStatus() != null && child.getStatus() == 1 && blogId != null) {
                blogMapper.update(null,
                        new LambdaUpdateWrapper<SysBlog>()
                                .eq(SysBlog::getId, blogId)
                                .setSql("comment_count = GREATEST(comment_count - 1, 0)"));
                log.info("文章评论数已减少（级联删除子评论），blogId={}", blogId);
            }
        }
    }

    /**
     * 清除评论缓存
     */
    private void clearCommentCache(Long commentId) {
        commentCache.invalidate(commentId);
        // 清除所有用户评论列表缓存
        userCommentListCache.invalidateAll();
        log.debug("评论缓存已清除，id={}", commentId);
    }

    /**
     * 将实体转换为响应DTO（用户自己的评论列表，不包含敏感信息）
     */
    private CommentResponseDTO toResponseDTO(SysComment comment) {
        CommentResponseDTO dto = new CommentResponseDTO();
        dto.setId(comment.getId());
        dto.setBlogId(comment.getBlogId());
        dto.setParentId(comment.getParentId());
        dto.setWebsite(comment.getWebsite());
        dto.setContent(comment.getContent());
        dto.setStatus(comment.getStatus() != null ? comment.getStatus().intValue() : null);
        dto.setLikeCount(comment.getLikeCount());
        dto.setDeviceInfo(comment.getDeviceInfo());
        dto.setIpAddress(comment.getIpAddress());
        dto.setIsAdmin(comment.getAdmin());
        dto.setCreateTime(comment.getCreateTime());
        dto.setUpdateTime(comment.getUpdateTime());

        // 根据文章ID获取文章标题
        if (comment.getBlogId() != null) {
            SysBlog blog = blogMapper.selectById(comment.getBlogId());
            if (blog != null) {
                dto.setBlogTitle(blog.getTitle());
            }
        }

        return dto;
    }

    /**
     * 构建状态筛选项
     */
    private Map<String, List<FilterOptionItem>> buildStatusFilterOptions() {
        List<FilterOptionItem> statusOptions = List.of(
                new FilterOptionItem(0, "待审核"),
                new FilterOptionItem(1, "已通过"),
                new FilterOptionItem(2, "垃圾评论"));
        return Map.of("status", statusOptions);
    }

    /**
     * 检查所有父评论是否都为通过状态
     * 递归向上查找所有父评论，如果有任意一个父评论不是已通过状态(1)，返回false
     */
    private boolean areAllParentCommentsApproved(SysComment comment) {
        Long parentId = comment.getParentId();

        while (parentId != null && parentId != 0) {
            SysComment parentComment = commentMapper.selectById(parentId);
            if (parentComment == null) {
                break;
            }
            // 如果父评论不是已通过状态，返回false
            if (parentComment.getStatus() != 1) {
                return false;
            }
            parentId = parentComment.getParentId();
        }

        return true;
    }

    @Override
    public void clearUserCommentListCache() {
        userCommentListCache.invalidateAll();
        log.debug("用户评论列表缓存已全部清除");
    }

    @Override
    public SaResult getReplyComments(Long userId, Integer limit) {
        try {
            // 限制最大返回条数为100
            int actualLimit = Math.min(limit != null ? limit : 10, 100);

            // 第一步：获取当前用户所有的已通过审核的评论ID（作为父评论）
            // 只查询已审核通过的评论（status=1），未通过的评论不应显示回复
            // 注意：isDeleted 由 @TableLogic 注解自动处理，无需显式添加
            List<Long> userCommentIds = commentMapper.selectList(
                    new LambdaQueryWrapper<SysComment>()
                            .eq(SysComment::getUserId, userId)
                            .eq(SysComment::getStatus, (byte) 1))
                    .stream()
                    .map(SysComment::getId)
                    .collect(Collectors.toList());

            if (userCommentIds.isEmpty()) {
                log.debug("用户暂无评论，无回复评论可返回，userId={}", userId);
                return SaResult.data(List.of());
            }

            // 第二步：查询所有以用户评论为父评论的回复评论（parentId in userCommentIds）
            // 这些回复评论的发布者不是当前用户（排除自己回复自己的情况）
            // 只查询已审核通过的评论（status=1）
            // 注意：isDeleted 由 @TableLogic 注解自动处理，无需显式添加
            LambdaQueryWrapper<SysComment> queryWrapper = new LambdaQueryWrapper<SysComment>()
                    .in(SysComment::getParentId, userCommentIds)
                    .eq(SysComment::getStatus, (byte) 1)
                    .orderByDesc(SysComment::getCreateTime)
                    .last("LIMIT " + actualLimit);

            // 如果回复者的userId不为空且等于当前用户，才排除（处理匿名评论情况）
            queryWrapper.and(w -> w.isNull(SysComment::getUserId).or().ne(SysComment::getUserId, userId));

            List<SysComment> replyComments = commentMapper.selectList(queryWrapper);

            // 第三步：转换为响应DTO列表
            List<PublicCommentResponseDTO> responseList = replyComments.stream()
                    .map(this::toReplyResponseDTO)
                    .collect(Collectors.toList());

            log.info("获取用户回复评论成功，userId={}，count={}", userId, responseList.size());
            return SaResult.data(responseList);

        } catch (Exception e) {
            log.error("获取用户回复评论异常", e);
            return SaResult.error("获取回复评论失败").setCode(500);
        }
    }

    /**
     * 将评论实体转换为回复评论响应DTO
     * 返回格式参考用户提供的API字段
     */
    private PublicCommentResponseDTO toReplyResponseDTO(SysComment comment) {
        PublicCommentResponseDTO dto = new PublicCommentResponseDTO();
        dto.setId(comment.getId());
        dto.setParentId(comment.getParentId());
        dto.setUsername(comment.getUsername());
        dto.setEmail(comment.getEmail());
        dto.setAvatarUrl(comment.getAvatarUrl());
        dto.setWebsite(comment.getWebsite());
        dto.setContent(comment.getContent());
        dto.setIsAdmin(comment.getAdmin());
        dto.setDeviceInfo(comment.getDeviceInfo());
        dto.setCreateTime(comment.getCreateTime());
        dto.setUpdateTime(comment.getUpdateTime());
        dto.setChildren(null); // 回复评论不需要返回子评论
        return dto;
    }
}
