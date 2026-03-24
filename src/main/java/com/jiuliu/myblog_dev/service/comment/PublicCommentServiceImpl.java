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
import com.jiuliu.myblog_dev.entity.blog.SysBlog;
import com.jiuliu.myblog_dev.entity.blog.comment.SysComment;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.mapper.blog.SysBlogMapper;
import com.jiuliu.myblog_dev.mapper.blog.comment.SysCommentMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 公共评论服务实现类（无需登录）
 */
@Service
public class PublicCommentServiceImpl implements PublicCommentService {

    private static final Logger log = LoggerFactory.getLogger(PublicCommentServiceImpl.class);

    private final SysCommentMapper commentMapper;
    private final SysBlogMapper blogMapper;
    private final SysUserMapper userMapper;

    public PublicCommentServiceImpl(SysCommentMapper commentMapper,
                                    SysBlogMapper blogMapper,
                                    SysUserMapper userMapper) {
        this.commentMapper = commentMapper;
        this.blogMapper = blogMapper;
        this.userMapper = userMapper;
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
                            .eq(SysBlog::getHidden, false)
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
            }

            // 3. 构建评论实体
            SysComment comment = new SysComment();
            comment.setBlogId(dto.getBlogId());
            comment.setParentId(parentId != null ? parentId : 0L);
            comment.setContent(dto.getContent());

            // 4. 处理已登录用户和游客的差异化字段
            if (isLogin && userId != null) {
                // 已登录用户：使用用户表的信息
                SysUser user = userMapper.selectById(userId);
                if (user != null) {
                    comment.setUserId(userId);
                    comment.setUsername(user.getNickname());
                    comment.setEmail(user.getEmail());
                    comment.setAvatarUrl(user.getAvatarUrl());
                } else {
                    // 用户不存在，回退到游客模式
                    comment.setUsername(dto.getUsername());
                    comment.setEmail(dto.getEmail());
                    comment.setAvatarUrl(dto.getAvatarUrl());
                }
                comment.setAdmin(isAdmin);
            } else {
                // 游客：使用传入的信息
                if (!StringUtils.hasText(dto.getUsername())) {
                    return SaResult.error("评论者名称不能为空").setCode(400);
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

            log.info("评论提交成功：commentId={}, blogId={}, parentId={}, isLogin={}",
                    comment.getId(), dto.getBlogId(), parentId, isLogin);

            // 6. 返回结果
            java.util.HashMap<String, Object> result = new java.util.HashMap<>();
            result.put("id", comment.getId());
            result.put("message", isLogin ? "评论提交成功" : "评论提交成功，待审核后显示");
            return SaResult.data(result);

        } catch (Exception e) {
            log.error("评论提交异常，blogId={}", dto.getBlogId(), e);
            return SaResult.error("评论提交失败").setCode(500);
        }
    }
}
