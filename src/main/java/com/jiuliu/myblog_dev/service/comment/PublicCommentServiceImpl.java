/*
 * [PublicCommentServiceImpl.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/24
 */

package com.jiuliu.myblog_dev.service.comment;

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuliu.myblog_dev.dto.comment.PublicCommentCreateDTO;
import com.jiuliu.myblog_dev.dto.comment.PublicCommentResponseDTO;
import com.jiuliu.myblog_dev.entity.blog.SysBlog;
import com.jiuliu.myblog_dev.entity.blog.comment.SysComment;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.mapper.blog.SysBlogMapper;
import com.jiuliu.myblog_dev.mapper.blog.comment.SysCommentMapper;
import com.jiuliu.myblog_dev.mapper.config.SysConfigMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRolePermissionMapper;
import com.jiuliu.myblog_dev.service.blog.PublicArticleService;
import com.jiuliu.myblog_dev.service.mail.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 公共评论服务实现类（无需登录）
 */
@Service
public class PublicCommentServiceImpl implements PublicCommentService {

    private static final Logger log = LoggerFactory.getLogger(PublicCommentServiceImpl.class);

    private static final String KEY_SHOW_EMAIL_ENABLED = "comment-show-email-enabled";
    private static final String KEY_SITE_DOMAIN = "site.domain";
    private static final String PERMISSION_COMMENT_LIST = "system:comment:list";

    /**
     * 评论最大嵌套层级配置（默认为1，即只能回复顶级评论）
     */
    @Value("${app.comment.max-nest-level:1}")
    private int maxNestLevel;

    private final SysCommentMapper commentMapper;
    private final SysBlogMapper blogMapper;
    private final SysUserMapper userMapper;
    private final SysConfigMapper sysConfigMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final PublicArticleService publicArticleService;
    private final MailService mailService;
    private final CommentService commentService;

    public PublicCommentServiceImpl(SysCommentMapper commentMapper,
                                    SysBlogMapper blogMapper,
                                    SysUserMapper userMapper,
                                    SysConfigMapper sysConfigMapper,
                                    SysRolePermissionMapper rolePermissionMapper,
                                    PublicArticleService publicArticleService,
                                    MailService mailService,
                                    CommentService commentService) {
        this.commentMapper = commentMapper;
        this.blogMapper = blogMapper;
        this.userMapper = userMapper;
        this.sysConfigMapper = sysConfigMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.publicArticleService = publicArticleService;
        this.mailService = mailService;
        this.commentService = commentService;
    }

    @Override
    @Transactional
    public SaResult createComment(PublicCommentCreateDTO dto, String ipAddress, String deviceInfo,
                                  boolean isLogin, Long userId, boolean isAdmin) {
        try {
            // 1. 验证文章是否存在且未隐藏
            SysBlog blog = blogMapper.selectOne(
                    new LambdaQueryWrapper<SysBlog>()
                            .eq(SysBlog::getId, dto.getBlogId())
//                            .and(w -> w.eq(SysBlog::getHidden, false).or().isNull(SysBlog::getHidden))
            );

            if (blog == null) {
                log.warn("评论提交失败：文章不存在或已隐藏，blogId={}", dto.getBlogId());
                return SaResult.error("文章不存在或已下架").setCode(404);
            }

            // 2. 如果是回复评论，验证父评论是否存在且属于同一篇文章
            Long parentId = dto.getParentId();
            if (parentId != null && parentId != 0) {
                SysComment parentComment = commentMapper.selectOne(
                        new LambdaQueryWrapper<SysComment>()
                                .eq(SysComment::getId, parentId)
                                .eq(SysComment::getBlogId, dto.getBlogId())
                                .eq(SysComment::getStatus, 1) // 只允许回复已审核通过的评论
                );

                if (parentComment == null) {
                    log.warn("评论提交失败：父评论不存在或不属于该文章，parentId={}, blogId={}", parentId, dto.getBlogId());
                    return SaResult.error("父评论不存在或不属于该文章").setCode(400);
                }

                // 3. 检查嵌套层级是否超过限制
                int parentLevel = calculateCommentLevel(parentComment);
                if (parentLevel >= maxNestLevel) {
                    log.warn("评论提交失败：嵌套层级超过限制，parentId={}, parentLevel={}, maxNestLevel={}",
                            parentId, parentLevel, maxNestLevel);
                    return SaResult.error("回复层级已达上限，最多支持 " + maxNestLevel + " 层嵌套").setCode(400);
                }
            }

            // 3. 构建评论实体
            SysComment comment = new SysComment();
            comment.setBlogId(dto.getBlogId());
            comment.setParentId(parentId != null ? parentId : 0L);
            comment.setContent(dto.getContent());

            // 4. 处理已登录用户和游客的差异化字段
            if (isLogin && userId != null) {
                // 已登录用户：userId 关联用户表，评论记录中个人字段为空
                SysUser user = userMapper.selectById(userId);
                if (user != null) {
                    comment.setUserId(userId);
                    comment.setUsername(null);   // 空，通过 userId 关联查询
                    comment.setEmail(null);       // 空，通过 userId 关联查询
                    comment.setAvatarUrl(null);   // 空，通过 userId 关联查询
                    comment.setAdmin(isAdmin);
                } else {
                    // 用户不存在，登录状态异常
                    log.warn("评论提交失败：用户已登录但无法获取用户信息，userId={}", userId);
                    return SaResult.error("用户信息获取失败，请重新登录").setCode(401);
                }
            } else {
                // 游客：使用传入的信息
                if (!StringUtils.hasText(dto.getUsername())) {
                    return SaResult.error("评论者名称不能为空").setCode(400);
                }

                // 邮箱必填校验
                if (!StringUtils.hasText(dto.getEmail())) {
                    return SaResult.error("邮箱不能为空").setCode(400);
                }

                // 邮箱格式验证
                if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
                    if (isEmailInvalid(dto.getEmail())) {
                        log.warn("评论提交失败：邮箱格式无效，email={}", dto.getEmail());
                        return SaResult.error("邮箱格式无效，请输入有效的邮箱地址").setCode(400);
                    }
                }

                // URL 格式验证
                if (dto.getAvatarUrl() != null && !dto.getAvatarUrl().trim().isEmpty()) {
                    if (isUrlInvalid(dto.getAvatarUrl())) {
                        log.warn("评论提交失败：头像URL格式无效，avatarUrl={}", dto.getAvatarUrl());
                        return SaResult.error("头像URL格式无效，请输入有效的网址").setCode(400);
                    }
                }

                if (dto.getWebsite() != null && !dto.getWebsite().trim().isEmpty()) {
                    if (isUrlInvalid(dto.getWebsite())) {
                        log.warn("评论提交失败：网站URL格式无效，website={}", dto.getWebsite());
                        return SaResult.error("网站URL格式无效，请输入有效的网址").setCode(400);
                    }
                }

                comment.setUserId(null);
                comment.setUsername(dto.getUsername());
                comment.setEmail(dto.getEmail());
                comment.setAvatarUrl(dto.getAvatarUrl());
                comment.setAdmin(false);
            }

            comment.setWebsite(dto.getWebsite());
            comment.setIpAddress(ipAddress);
            comment.setDeviceInfo(deviceInfo);
            comment.setStatus((byte) 1); // 已登录用户直接通过审核，游客待审核（根据需求可调整）
            if (!isLogin) {
                comment.setStatus((byte) 0); // 游客评论待审核
            }

            // 5. 保存评论
            commentMapper.insert(comment);

            // 6. 更新文章的评论数
            blogMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysBlog>()
                            .eq(SysBlog::getId, dto.getBlogId())
                            .setSql("comment_count = comment_count + 1")
            );

            // 7. 清除公共文章缓存（评论数已更新）
            publicArticleService.clearPublicArticleCache();

            // 7.5 清除用户评论列表缓存（新评论发布后，用户中心评论列表需要刷新）
            if (isLogin && userId != null) {
                commentService.clearUserCommentListCache();
                log.debug("用户评论列表缓存已清除，userId={}", userId);
            }

            log.info("评论提交成功：commentId={}, blogId={}, parentId={}, isLogin={}",
                    comment.getId(), dto.getBlogId(), parentId, isLogin);

            // 8. 发送新评论通知给管理员
            boolean adminNotified = sendNewCommentNotificationToAdmins(comment, blog, dto.getUsername());
            log.debug("管理员通知发送结果，commentId={}, notified={}", comment.getId(), adminNotified);

            // 9. 返回结果
            java.util.HashMap<String, Object> result = new java.util.HashMap<>();
            result.put("id", comment.getId());
            result.put("message", isLogin ? "评论提交成功" : "评论提交成功，待审核后显示");
            return SaResult.data(result);

        } catch (Exception e) {
            log.error("评论提交异常，blogId={}", dto.getBlogId(), e);
            return SaResult.error("评论提交失败").setCode(500);
        }
    }

    @Override
    public List<PublicCommentResponseDTO> getCommentsByBlogId(Long blogId) {
        // 1. 验证文章是否存在且未隐藏
        SysBlog blog = blogMapper.selectOne(
                new LambdaQueryWrapper<SysBlog>()
                        .eq(SysBlog::getId, blogId)
//                        .and(w -> w.eq(SysBlog::getHidden, false).or().isNull(SysBlog::getHidden))
        );

        if (blog == null) {
            log.warn("获取评论失败：文章不存在或已隐藏，blogId={}", blogId);
            return new ArrayList<>();
        }

        // 2. 查询该文章下所有 status=1 且未删除的评论
        List<SysComment> allComments = commentMapper.selectList(
                new LambdaQueryWrapper<SysComment>()
                        .eq(SysComment::getBlogId, blogId)
                        .eq(SysComment::getStatus, (byte) 1)
                        .orderByAsc(SysComment::getCreateTime)
        );

        if (allComments.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 收集所有评论的 userId 用于批量查询用户信息
        List<Long> userIds = allComments.stream()
                .map(SysComment::getUserId)
                .filter(userId -> userId != null && userId > 0)
                .distinct()
                .collect(Collectors.toList());

        // 4. 批量查询用户信息
        Map<Long, SysUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUser> users = userMapper.selectList(
                    new LambdaQueryWrapper<SysUser>().in(SysUser::getId, userIds)
            );
            userMap = users.stream()
                    .collect(Collectors.toMap(SysUser::getId, u -> u));
        }

        // 5. 构建 parentId -> 子评论列表 的映射
        Map<Long, List<PublicCommentResponseDTO>> childrenMap = new HashMap<>();
        List<PublicCommentResponseDTO> topLevelComments = new ArrayList<>();

        for (SysComment comment : allComments) {
            PublicCommentResponseDTO dto = convertToResponseDTO(comment, userMap);
            Long parentId = comment.getParentId();

//            if (parentId != null && parentId > 0 && userMap.containsKey(comment.getUserId())) {
//                // 有有效的 parentId 且评论者有对应用户，使用用户表信息（已在 convertToResponseDTO 中处理）
//            }

            if (parentId != null && parentId > 0) {
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(dto);
            } else {
                topLevelComments.add(dto);
            }
        }

        // 6. 将子评论设置到父评论的 children 字段
        for (PublicCommentResponseDTO comment : topLevelComments) {
            comment.setChildren(childrenMap.get(comment.getId()));
            setChildrenRecursively(comment, childrenMap);
        }

        return topLevelComments;
    }

    /**
     * 递归设置评论的子评论
     */
    private void setChildrenRecursively(PublicCommentResponseDTO comment, Map<Long, List<PublicCommentResponseDTO>> childrenMap) {
        List<PublicCommentResponseDTO> children = childrenMap.get(comment.getId());
        if (children != null && !children.isEmpty()) {
            comment.setChildren(children);
            for (PublicCommentResponseDTO child : children) {
                setChildrenRecursively(child, childrenMap);
            }
        }
    }

    /**
     * 将 SysComment 转换为 PublicCommentResponseDTO
     * 如果评论有有效的 userId 且对应用户存在，则使用用户表信息覆盖
     * 根据配置决定是否隐藏邮箱
     */
    private PublicCommentResponseDTO convertToResponseDTO(SysComment comment, Map<Long, SysUser> userMap) {
        PublicCommentResponseDTO dto = new PublicCommentResponseDTO();
        dto.setId(comment.getId());
        dto.setParentId(comment.getParentId());
        dto.setWebsite(comment.getWebsite());
        dto.setContent(comment.getContent());
        dto.setIsAdmin(comment.getAdmin());
        dto.setDeviceInfo(comment.getDeviceInfo());
        dto.setCreateTime(comment.getCreateTime());
        dto.setUpdateTime(comment.getUpdateTime());

        // 如果评论者有对应的用户且用户有效，使用用户表信息
        Long userId = comment.getUserId();
        String email;
        if (userId != null && userId > 0 && userMap.containsKey(userId)) {
            SysUser user = userMap.get(userId);
            dto.setUsername(user.getNickname());
            email = user.getEmail();
            dto.setAvatarUrl(user.getAvatarUrl());
        } else {
            // 使用评论原始信息（游客或用户已被删除）
            dto.setUsername(comment.getUsername());
            email = comment.getEmail();
            dto.setAvatarUrl(comment.getAvatarUrl());
        }

        // 根据配置决定是否隐藏邮箱
        dto.setEmail(maskEmailIfNeeded(email));

        return dto;
    }

    /**
     * 根据配置决定是否隐藏邮箱
     * 如果 comment-show-email-enabled 为 false，则隐藏邮箱中间部分
     */
    private String maskEmailIfNeeded(String email) {
        if (!StringUtils.hasText(email)) {
            return email;
        }

        // 检查配置：comment-show-email-enabled 是否为 true
        boolean showFullEmail = isShowEmailEnabled();
        if (showFullEmail) {
            return email;
        }

        // 不显示邮箱
        return "";
    }

//    /**
//     * 隐藏邮箱中间部分
//     * 格式: 前3位 + * + @前保留3位 + @ + @后保留2位
//     * 例如: 3201234567@qq.com -> 320*******67@qq.com
//     */
//    private String maskEmail(String email) {
//        if (!StringUtils.hasText(email)) {
//            return email;
//        }
//
//        int atIndex = email.indexOf('@');
//        if (atIndex <= 0) {
//            // 无效邮箱格式，返回原值
//            return email;
//        }
//
//        String localPart = email.substring(0, atIndex);
//        String domainPart = email.substring(atIndex);
//
//        // 如果本地部分太短（不足6位），直接返回原值（无法隐藏中间部分）
//        if (localPart.length() < 6) {
//            return email;
//        }
//
//        // 保留前3位，中间用 * 填充，末尾保留3位
//        String prefix = localPart.substring(0, 3);
//        int maskLength = Math.min(localPart.length() - 6, 7); // 中间星号数量，最多7个
//        String maskedLocal = prefix + "*".repeat(maskLength) + localPart.substring(localPart.length() - 3);
//
//        return maskedLocal + domainPart;
//    }

    /**
     * 检查是否启用了显示完整邮箱
     */
    private boolean isShowEmailEnabled() {
        try {
            String value = sysConfigMapper.selectValueByKey(KEY_SHOW_EMAIL_ENABLED);
            return "true".equalsIgnoreCase(value);
        } catch (Exception e) {
            log.debug("获取邮箱显示配置失败，使用默认值 false: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 发送新评论通知给管理员
     * 无论审核通过还是未通过，只要是新评论都会通知
     * 如果是游客评论，会在通知中提示需要审核
     */
    private boolean sendNewCommentNotificationToAdmins(SysComment comment, SysBlog blog, String commenterName) {
        try {
            // 检查评论通知是否启用
            boolean notificationEnabled = mailService.isCommentNotificationEnabled();
            if (!notificationEnabled) {
                log.debug("新评论通知跳过：评论通知功能未启用");
                return false;
            }

            // 获取拥有 system:comment:list 权限的用户邮箱列表
            List<String> adminEmails = rolePermissionMapper.selectUserEmailsByPermissionCode(PERMISSION_COMMENT_LIST);
            if (adminEmails == null || adminEmails.isEmpty()) {
                log.debug("新评论通知跳过：没有管理员邮箱，commentId={}", comment.getId());
                return false;
            }

            // 获取网站域名
            String siteDomain = sysConfigMapper.selectValueByKey(KEY_SITE_DOMAIN);

            // 判断是否是游客评论
            boolean isGuest = (comment.getUserId() == null);

            // 获取评论者名称
            String commenter = isGuest ? commenterName : getCommenterName(comment);

            // 发送邮件通知
            SaResult result = mailService.sendNewCommentNotificationToAdmins(
                    adminEmails,
                    siteDomain,
                    isGuest,
                    comment.getId(),
                    blog.getId(),
                    blog.getTitle(),
                    commenter,
                    comment.getContent()
            );

            boolean sent = (result.getCode() == 200);
            if (sent) {
                log.info("新评论通知已发送给管理员，commentId={}, isGuest={}, adminCount={}",
                        comment.getId(), isGuest, adminEmails.size());
            } else {
                log.warn("新评论通知发送失败，commentId={}, error={}", comment.getId(), result.getMsg());
            }
            return sent;

        } catch (Exception e) {
            // 邮件发送失败不影响主流程，只记录日志
            log.error("发送新评论通知邮件异常，commentId={}", comment.getId(), e);
            return false;
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
     * 计算评论的层级（从1开始）
     * 顶级评论(parentId=0)层级为1，一级子评论层级为2，以此类推
     */
    private int calculateCommentLevel(SysComment comment) {
        int level = 1;
        Long parentId = comment.getParentId();

        while (parentId != null && parentId != 0) {
            SysComment parentComment = commentMapper.selectOne(
                    new LambdaQueryWrapper<SysComment>()
                            .eq(SysComment::getId, parentId)
            );
            if (parentComment == null) {
                break;
            }
            level++;
            parentId = parentComment.getParentId();
        }

        return level;
    }

    /**
     * 验证 URL 格式是否有效
     * 支持 http:// 和 https:// 协议
     */
    private boolean isUrlInvalid(String url) {
        if (url == null || url.trim().isEmpty()) {
            return true;
        }
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            String protocol = parsedUrl.getProtocol();
            return !"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol);
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 验证邮箱格式是否有效
     */
    private boolean isEmailInvalid(String email) {
        if (email == null || email.trim().isEmpty()) {
            return true;
        }
        // 简单的邮箱格式正则：必须包含 @ 和 .，且 @ 不能在首位
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return !email.matches(emailRegex);
    }
}
