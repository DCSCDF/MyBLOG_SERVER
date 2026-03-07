/*
 * [GlobalCommentServiceImpl.java]
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
import com.jiuliu.myblog_dev.dto.comment.CommentStatusUpdateDTO;
import com.jiuliu.myblog_dev.dto.comment.CommentUpdateDTO;
import com.jiuliu.myblog_dev.dto.comment.PageGlobalCommentDTO;
import com.jiuliu.myblog_dev.dto.comment.PageGlobalCommentResponseDTO;
import com.jiuliu.myblog_dev.dto.common.FilterOptionItem;
import com.jiuliu.myblog_dev.entity.blog.SysBlog;
import com.jiuliu.myblog_dev.entity.blog.comment.SysComment;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.mapper.blog.SysBlogMapper;
import com.jiuliu.myblog_dev.mapper.blog.comment.SysCommentMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
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
 * 全局评论服务实现类（管理员）
 */
@Service
public class GlobalCommentServiceImpl implements GlobalCommentService {

    private static final Logger log = LoggerFactory.getLogger(GlobalCommentServiceImpl.class);

    /**
     * 全局评论缓存 - 缓存单个评论，key为评论ID，value为SysComment对象
     * 缓存时间：10分钟
     */
    private final Cache<Long, SysComment> globalCommentCache = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    /**
     * 全局评论列表缓存
     * 缓存时间：5分钟
     */
    private final Cache<String, PageGlobalCommentResponseDTO> globalCommentListCache = CacheBuilder.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    private final SysCommentMapper commentMapper;
    private final SysBlogMapper blogMapper;
    private final SysUserMapper userMapper;

    public GlobalCommentServiceImpl(SysCommentMapper commentMapper, SysBlogMapper blogMapper, SysUserMapper userMapper) {
        this.commentMapper = commentMapper;
        this.blogMapper = blogMapper;
        this.userMapper = userMapper;
    }

    @Override
    public SaResult getPageGlobalComments(PageGlobalCommentDTO dto) {
        try {
            // 构建缓存键
            String cacheKey = CacheUtil.CACHE_KEY_GLOBAL_COMMENT_LIST + dto.getCurrentPage() + "-" + dto.getPageSize() + "-" + dto.getStatus() + "-" + dto.getKeyword();

            // 尝试从缓存获取
            PageGlobalCommentResponseDTO cached = globalCommentListCache.getIfPresent(cacheKey);
            if (cached != null) {
                log.debug("从缓存获取全局评论列表，key={}", cacheKey);
                return SaResult.data(cached);
            }

            LambdaQueryWrapper<SysComment> wrapper = new LambdaQueryWrapper<SysComment>()
                    .orderByDesc(SysComment::getCreateTime);

            // 关键词搜索
            if (StringUtils.hasText(dto.getKeyword())) {
                String kw = dto.getKeyword().trim();
                wrapper.and(w -> w.like(SysComment::getUsername, kw)
                        .or().like(SysComment::getEmail, kw)
                        .or().like(SysComment::getContent, kw));
            }

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
            PageGlobalCommentResponseDTO response = new PageGlobalCommentResponseDTO();
            response.setRecords(records);
            response.setTotal(pageResult.getTotal());
            response.setSize(pageResult.getSize());
            response.setCurrent(pageResult.getCurrent());
            response.setPages(pageResult.getPages());
            response.setFilterOptions(buildStatusFilterOptions());

            // 存入缓存
            globalCommentListCache.put(cacheKey, response);

            return SaResult.data(response);
        } catch (Exception e) {
            log.error("分页获取全局评论列表异常", e);
            return SaResult.error("获取评论列表失败").setCode(500);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult updateComment(CommentUpdateDTO dto) {
        SysComment existing = commentMapper.selectById(dto.getId());
        if (existing == null) {
            log.warn("全局更新评论失败：评论不存在，id={}", dto.getId());
            return SaResult.error("评论不存在").setCode(404);
        }

        // 更新评论
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
        log.info("全局评论更新成功，id={}", dto.getId());

        // 清除缓存
        clearGlobalCommentCache(dto.getId());

        SysComment updated = commentMapper.selectById(dto.getId());
        return SaResult.data(toResponseDTO(updated));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult deleteComment(Long commentId) {
        SysComment existing = commentMapper.selectById(commentId);
        if (existing == null) {
            log.warn("删除评论失败：评论不存在，id={}", commentId);
            return SaResult.error("评论不存在").setCode(404);
        }

        // 逻辑删除：设置 is_deleted = 1
        commentMapper.update(null, new LambdaUpdateWrapper<SysComment>()
                .eq(SysComment::getId, commentId)
                .set(SysComment::getIsDeleted, 1)
                .set(SysComment::getUpdateTime, LocalDateTime.now()));

        log.info("全局评论删除成功，id={}", commentId);

        // 清除缓存
        clearGlobalCommentCache(commentId);

        return SaResult.data("删除成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult approveComment(Long commentId, CommentStatusUpdateDTO dto) {
        SysComment existing = commentMapper.selectById(commentId);
        if (existing == null) {
            log.warn("审核评论失败：评论不存在，id={}", commentId);
            return SaResult.error("评论不存在").setCode(404);
        }

        Integer newStatus = dto.getStatus();
        if (newStatus == null || (newStatus != 0 && newStatus != 1 && newStatus != 2)) {
            return SaResult.error("状态值无效，仅支持 0=待审核，1=已通过，2=垃圾评论").setCode(400);
        }

        commentMapper.update(null, new LambdaUpdateWrapper<SysComment>()
                .eq(SysComment::getId, commentId)
                .set(SysComment::getStatus, newStatus)
                .set(SysComment::getUpdateTime, LocalDateTime.now()));

        log.info("评论审核状态变更成功，id={}, 新状态={}", commentId, newStatus);

        // 清除缓存
        clearGlobalCommentCache(commentId);

        SysComment updated = commentMapper.selectById(commentId);
        return SaResult.data(toResponseDTO(updated));
    }

    /**
     * 清除全局评论缓存
     */
    private void clearGlobalCommentCache(Long commentId) {
        globalCommentCache.invalidate(commentId);
        globalCommentListCache.invalidateAll();
        log.debug("全局评论缓存已清除，id={}", commentId);
    }

    /**
     * 将实体转换为响应DTO（全局评论列表）
     * 如果 user_id 不为空且能对应上用户表，则使用用户表的信息（username对应nickname）
     * 否则使用评论表中的信息
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

        // 处理用户名、邮箱、头像：如果user_id不为空且能对应上用户表，则使用用户表信息
        if (comment.getUserId() != null) {
            SysUser user = userMapper.selectById(comment.getUserId());
            if (user != null) {
                // 使用用户表的昵称
                dto.setUsername(user.getNickname());
                // 邮箱和头像也使用用户表的
                dto.setEmail(user.getEmail());
                dto.setAvatarUrl(user.getAvatarUrl());
            } else {
                // 用户不存在，使用评论表中的信息
                dto.setUsername(comment.getUsername());
                dto.setEmail(comment.getEmail());
                dto.setAvatarUrl(comment.getAvatarUrl());
            }
        } else {
            // user_id为空（访客），使用评论表中的信息
            dto.setUsername(comment.getUsername());
            dto.setEmail(comment.getEmail());
            dto.setAvatarUrl(comment.getAvatarUrl());
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
