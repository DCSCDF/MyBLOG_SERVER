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
import com.jiuliu.myblog_dev.dto.comment.*;
import com.jiuliu.myblog_dev.dto.common.FilterOptionItem;
import com.jiuliu.myblog_dev.entity.blog.SysBlog;
import com.jiuliu.myblog_dev.entity.blog.comment.SysComment;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.mapper.blog.SysBlogMapper;
import com.jiuliu.myblog_dev.mapper.blog.comment.SysCommentMapper;
import com.jiuliu.myblog_dev.mapper.config.SysConfigMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.service.blog.PublicArticleService;
import com.jiuliu.myblog_dev.service.mail.MailService;
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
    private final MailService mailService;
    private final SysConfigMapper sysConfigMapper;
    private final PublicArticleService publicArticleService;

    private static final String KEY_SITE_DOMAIN = "site.domain";

    public GlobalCommentServiceImpl(SysCommentMapper commentMapper, SysBlogMapper blogMapper,
                                    SysUserMapper userMapper, MailService mailService,
                                    SysConfigMapper sysConfigMapper,
                                    PublicArticleService publicArticleService) {
        this.commentMapper = commentMapper;
        this.blogMapper = blogMapper;
        this.userMapper = userMapper;
        this.mailService = mailService;
        this.sysConfigMapper = sysConfigMapper;
        this.publicArticleService = publicArticleService;
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

        // 如果是子评论，检查父评论链是否都通过审核
        if (existing.getParentId() != null && existing.getParentId() != 0) {
            if (areAllParentCommentsApproved(existing)) {
                // 父评论链中有未通过的评论，子评论只能是待审核状态且无法修改内容
                log.warn("全局更新评论失败：父评论链中存在未通过的评论，commentId={}", dto.getId());
                return SaResult.error("父评论尚未通过审核，无法修改此回复").setCode(400);
            }
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

        // 编辑评论后，重新设置为待审核状态
        //updateWrapper.set(SysComment::getStatus, 0);

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

        // 级联删除：先删除所有子评论（递归）
        deleteChildComments(commentId);

        // 逻辑删除：设置 is_deleted = 1
        commentMapper.update(null, new LambdaUpdateWrapper<SysComment>()
                .eq(SysComment::getId, commentId)
                .set(SysComment::getIsDeleted, 1)
                .set(SysComment::getUpdateTime, LocalDateTime.now()));

        log.info("全局评论删除成功，id={}", commentId);

        // 清除公共文章缓存（评论数可能变化）
        clearPublicArticleCacheIfNeeded(existing);

        // 清除缓存
        clearGlobalCommentCache(commentId);

        return SaResult.data("删除成功");
    }

    /**
     * 递归删除子评论
     */
    private void deleteChildComments(Long parentId) {
        // 查询所有直接子评论
        List<SysComment> childComments = commentMapper.selectList(
                new LambdaQueryWrapper<SysComment>()
                        .eq(SysComment::getParentId, parentId)
        );

        for (SysComment child : childComments) {
            // 递归删除子评论的子评论
            deleteChildComments(child.getId());
            // 逻辑删除子评论
            commentMapper.update(null, new LambdaUpdateWrapper<SysComment>()
                    .eq(SysComment::getId, child.getId())
                    .set(SysComment::getIsDeleted, 1)
                    .set(SysComment::getUpdateTime, LocalDateTime.now()));
            clearGlobalCommentCache(child.getId());
            log.info("子评论级联删除成功，id={}", child.getId());
        }
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

        // 如果要设为已通过(1)或垃圾评论(2)，需要检查所有父评论是否都为通过状态
        if (newStatus == 1 || newStatus == 2) {
            if (areAllParentCommentsApproved(existing)) {
                log.warn("审核评论失败：存在未通过的父评论链，commentId={}", commentId);
                return SaResult.error("存在未通过的父评论，无法将状态设为已通过或垃圾评论").setCode(400);
            }
        }

        // 记录是否是顶级评论且即将通过审核
        boolean isTopLevelAndApproving = (newStatus == 1)
                && (existing.getParentId() == null || existing.getParentId() == 0);

        // 保存文章信息用于通知（在状态更新前获取）
        SysBlog blogForNotification = null;
        if (isTopLevelAndApproving && existing.getBlogId() != null) {
            blogForNotification = blogMapper.selectById(existing.getBlogId());
        }

        // 更新评论状态
        commentMapper.update(null, new LambdaUpdateWrapper<SysComment>()
                .eq(SysComment::getId, commentId)
                .set(SysComment::getStatus, newStatus)
                .set(SysComment::getUpdateTime, LocalDateTime.now()));

        // 如果父评论被设为待审核(0)或垃圾评论(2)，子评论也要设为待审核
        if (newStatus == 0 || newStatus == 2) {
            updateChildCommentsStatus(commentId, (byte) 0);
        }

        log.info("评论审核状态变更成功，id={}, 新状态={}", commentId, newStatus);

        // 清除公共文章缓存（评论数可能变化）
        clearPublicArticleCacheIfNeeded(existing);

        // 清除缓存
        clearGlobalCommentCache(commentId);

        // 发送邮件通知（仅在审核通过或垃圾评论时）
        if (newStatus == 1) {
            // 审核通过：发送审核通过通知给当前评论作者
            sendCommentNotification(existing, true);
            // 如果是子评论，发送回复通知给父评论作者
            if (existing.getParentId() != null && existing.getParentId() != 0) {
                sendReplyNotificationToParent(existing.getParentId(), existing.getContent());
            }
            // 如果是顶级评论通过审核，通知文章作者
            if (isTopLevelAndApproving && blogForNotification != null) {
                sendTopLevelCommentApprovedNotificationToAuthor(existing, blogForNotification);
            }
        } else if (newStatus == 2) {
            // 设为垃圾评论：发送未通过通知给当前评论作者
            sendCommentNotification(existing, false);
        }

        SysComment updated = commentMapper.selectById(commentId);
        return SaResult.data(toResponseDTO(updated));
    }

    /**
     * 发送评论审核结果邮件通知
     */
    private void sendCommentNotification(SysComment comment, boolean approved) {
        // 检查评论通知是否启用
        if (!mailService.isCommentNotificationEnabled()) {
            log.debug("评论审核通知跳过：评论通知功能未启用");
            return;
        }

        // 获取收件人邮箱
        String toEmail = getRecipientEmail(comment);
        if (!StringUtils.hasText(toEmail)) {
            log.debug("评论审核通知邮件跳过：无法获取收件人邮箱，commentId={}", comment.getId());
            return;
        }

        // 获取网站域名
        String siteDomain = getSiteDomain();

        // 发送邮件（异步执行，不影响主流程）
        try {
            SaResult result = mailService.sendCommentReviewNotification(toEmail, approved, siteDomain);
            if (result.getCode() == 200) {
                log.info("评论审核通知邮件发送成功，commentId={}, to={}, approved={}",
                        comment.getId(), toEmail, approved);
            } else {
                log.warn("评论审核通知邮件发送失败，commentId={}, to={}, error={}",
                        comment.getId(), toEmail, result.getMsg());
            }
        } catch (Exception e) {
            log.error("评论审核通知邮件发送异常，commentId={}", comment.getId(), e);
        }
    }

    /**
     * 获取评论的收件人邮箱
     * 优先使用用户表的邮箱，其次使用评论表的邮箱
     */
    private String getRecipientEmail(SysComment comment) {
        // 如果有 userId 且对应用户存在，使用用户表的邮箱
        if (comment.getUserId() != null) {
            SysUser user = userMapper.selectById(comment.getUserId());
            if (user != null && StringUtils.hasText(user.getEmail())) {
                return user.getEmail();
            }
        }
        // 否则使用评论表的邮箱
        if (StringUtils.hasText(comment.getEmail())) {
            return comment.getEmail();
        }
        return null;
    }

    /**
     * 获取网站域名配置
     */
    private String getSiteDomain() {
        try {
            return sysConfigMapper.selectValueByKey(KEY_SITE_DOMAIN);
        } catch (Exception e) {
            log.warn("获取网站域名配置失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 发送评论回复通知邮件给父评论作者
     *
     * @param parentCommentId 父评论ID
     * @param replyContent    回复的评论内容
     */
    private void sendReplyNotificationToParent(Long parentCommentId, String replyContent) {
        // 检查评论通知是否启用
        if (!mailService.isCommentNotificationEnabled()) {
            log.debug("评论回复通知跳过：评论通知功能未启用");
            return;
        }

        // 获取父评论
        SysComment parentComment = commentMapper.selectById(parentCommentId);
        if (parentComment == null) {
            log.debug("评论回复通知跳过：父评论不存在，parentId={}", parentCommentId);
            return;
        }

        // 获取父评论作者的邮箱
        String toEmail = getRecipientEmail(parentComment);
        if (!StringUtils.hasText(toEmail)) {
            log.debug("评论回复通知跳过：无法获取收件人邮箱，parentId={}", parentCommentId);
            return;
        }

        // 获取网站域名
        String siteDomain = getSiteDomain();

        // 发送邮件
        try {
            SaResult result = mailService.sendCommentReplyNotification(toEmail, siteDomain, replyContent);
            if (result.getCode() == 200) {
                log.info("评论回复通知邮件发送成功，parentId={}, to={}", parentCommentId, toEmail);
            } else {
                log.warn("评论回复通知邮件发送失败，parentId={}, to={}, error={}",
                        parentCommentId, toEmail, result.getMsg());
            }
        } catch (Exception e) {
            log.error("评论回复通知邮件发送异常，parentId={}", parentCommentId, e);
        }
    }

    /**
     * 发送顶级评论通过审核通知给文章作者
     * 只有顶级评论（parentId=0或null）通过审核时才通知文章作者
     *
     * @param comment 顶级评论
     * @param blog    文章
     */
    private void sendTopLevelCommentApprovedNotificationToAuthor(SysComment comment, SysBlog blog) {
        // 检查评论通知是否启用
        if (!mailService.isCommentNotificationEnabled()) {
            log.debug("顶级评论通过通知跳过：评论通知功能未启用");
            return;
        }

        // 获取文章作者
        if (blog.getAuthorId() == null) {
            log.debug("顶级评论通过通知跳过：文章没有作者，blogId={}", blog.getId());
            return;
        }

        SysUser author = userMapper.selectById(blog.getAuthorId());
        if (author == null) {
            log.debug("顶级评论通过通知跳过：文章作者不存在，authorId={}", blog.getAuthorId());
            return;
        }

        // 获取作者邮箱
        String toEmail = author.getEmail();
        if (!StringUtils.hasText(toEmail)) {
            log.debug("顶级评论通过通知跳过：作者邮箱为空，authorId={}", blog.getAuthorId());
            return;
        }

        // 获取网站域名
        String siteDomain = getSiteDomain();

        // 获取评论者名称
        String commenter = getCommenterName(comment);

        // 发送邮件
        try {
            SaResult result = mailService.sendTopLevelCommentApprovedNotification(
                    toEmail, siteDomain, blog.getId(), blog.getTitle(),
                    comment.getId(), comment.getContent(), commenter);
            if (result.getCode() == 200) {
                log.info("顶级评论通过通知发送给文章作者成功，commentId={}, authorId={}, authorEmail={}",
                        comment.getId(), blog.getAuthorId(), toEmail);
            } else {
                log.warn("顶级评论通过通知发送给文章作者失败，commentId={}, authorEmail={}, error={}",
                        comment.getId(), toEmail, result.getMsg());
            }
        } catch (Exception e) {
            log.error("顶级评论通过通知发送异常，commentId={}", comment.getId(), e);
        }
    }

    /**
     * 获取评论者名称
     */
    private String getCommenterName(SysComment comment) {
        if (comment.getUserId() != null) {
            SysUser user = userMapper.selectById(comment.getUserId());
            if (user != null && StringUtils.hasText(user.getNickname())) {
                return user.getNickname();
            }
        }
        if (StringUtils.hasText(comment.getUsername())) {
            return comment.getUsername();
        }
        return "匿名用户";
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
                return true;
            }
            parentId = parentComment.getParentId();
        }

        return false;
    }

    /**
     * 递归更新子评论状态
     */
    private void updateChildCommentsStatus(Long parentId, byte status) {
        List<SysComment> childComments = commentMapper.selectList(
                new LambdaQueryWrapper<SysComment>()
                        .eq(SysComment::getParentId, parentId)
        );

        for (SysComment child : childComments) {
            commentMapper.update(null, new LambdaUpdateWrapper<SysComment>()
                    .eq(SysComment::getId, child.getId())
                    .set(SysComment::getStatus, status)
                    .set(SysComment::getUpdateTime, LocalDateTime.now()));
            clearGlobalCommentCache(child.getId());
            log.info("子评论状态联动更新，id={}, 新状态={}", child.getId(), status);
            // 递归处理子评论的子评论
            updateChildCommentsStatus(child.getId(), status);
        }
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
     * 根据评论状态变更清除公共文章缓存
     * 当评论被审核通过(status=1)或取消通过(非1)时，需要刷新文章列表中的评论数
     */
    private void clearPublicArticleCacheIfNeeded(SysComment comment) {
        // 如果评论的博客ID存在，且状态变更为已通过(status=1)，清除公共文章缓存
        // 这样文章列表中的评论数会重新计算
        if (comment.getBlogId() != null) {
            publicArticleService.clearPublicArticleCache();
            log.debug("公共文章缓存已清除（评论审核状态变更），blogId={}, commentId={}", comment.getBlogId(), comment.getId());
        }
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
