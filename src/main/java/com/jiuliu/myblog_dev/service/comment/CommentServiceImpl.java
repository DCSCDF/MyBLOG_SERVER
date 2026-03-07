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
import com.jiuliu.myblog_dev.dto.comment.CommentResponseDTO;
import com.jiuliu.myblog_dev.dto.comment.CommentUpdateDTO;
import com.jiuliu.myblog_dev.dto.comment.PageCommentDTO;
import com.jiuliu.myblog_dev.dto.comment.PageCommentResponseDTO;
import com.jiuliu.myblog_dev.dto.common.FilterOptionItem;
import com.jiuliu.myblog_dev.entity.blog.SysBlog;
import com.jiuliu.myblog_dev.entity.blog.comment.SysComment;
import com.jiuliu.myblog_dev.mapper.blog.SysBlogMapper;
import com.jiuliu.myblog_dev.mapper.blog.comment.SysCommentMapper;
import com.jiuliu.myblog_dev.utils.CacheUtil;
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

    public CommentServiceImpl(SysCommentMapper commentMapper, SysBlogMapper blogMapper) {
        this.commentMapper = commentMapper;
        this.blogMapper = blogMapper;
    }

    @Override
    public SaResult getPageUserComments(PageCommentDTO dto, Long userId) {
        try {
            // 构建缓存键
            String cacheKey = CacheUtil.CACHE_KEY_USER_COMMENT_LIST + userId + "-" + dto.getCurrentPage() + "-" + dto.getPageSize() + "-" + dto.getStatus();

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

        // 逻辑删除：设置 is_deleted = 1
        commentMapper.update(null, new LambdaUpdateWrapper<SysComment>()
                .eq(SysComment::getId, commentId)
                .set(SysComment::getIsDeleted, 1)
                .set(SysComment::getUpdateTime, LocalDateTime.now()));

        log.info("评论删除成功，id={}", commentId);

        // 清除缓存
        clearCommentCache(commentId);

        return SaResult.data("删除成功");
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
                new FilterOptionItem(2, "垃圾评论")
        );
        return Map.of("status", statusOptions);
    }
}
