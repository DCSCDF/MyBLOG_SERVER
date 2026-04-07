/*
 * [MailService.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/6
 */

package com.jiuliu.myblog_dev.service.mail;

import cn.dev33.satoken.util.SaResult;

import java.util.List;

/**
 * 邮件服务接口
 */
public interface MailService {

    /**
     * 发送测试邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return SaResult
     */
    SaResult sendTestMail(String to, String subject, String content);

    /**
     * 检查 SMTP 配置是否完成
     *
     * @return SaResult
     */
    SaResult checkSmtpConfiguration();

    /**
     * 发送评论审核结果通知邮件
     *
     * @param to          收件人邮箱
     * @param approved    是否审核通过
     * @param siteDomain  网站域名
     * @return SaResult
     */
    SaResult sendCommentReviewNotification(String to, boolean approved, String siteDomain);

    /**
     * 发送评论回复通知邮件
     *
     * @param to          收件人邮箱
     * @param siteDomain  网站域名
     * @param replyContent 回复的评论内容
     * @return SaResult
     */
    SaResult sendCommentReplyNotification(String to, String siteDomain, String replyContent);

    /**
     * 检查评论通知功能是否启用
     *
     * @return true if enabled
     */
    boolean isCommentNotificationEnabled();

    /**
     * 发送新评论通知邮件给管理员
     *
     * @param toEmailList  收件人邮箱列表
     * @param siteDomain   网站域名
     * @param isGuest      是否是游客评论（需要审核）
     * @param commentId    评论ID
     * @param blogId       文章ID
     * @param blogTitle    文章标题
     * @param commenter    评论者名称
     * @param content      评论内容
     * @return SaResult
     */
    SaResult sendNewCommentNotificationToAdmins(List<String> toEmailList, String siteDomain,
                                                 boolean isGuest, Long commentId, Long blogId,
                                                 String blogTitle, String commenter, String content);

    /**
     * 发送顶级评论通过审核通知给文章作者
     *
     * @param toEmail      收件人邮箱
     * @param siteDomain   网站域名
     * @param blogId       文章ID
     * @param blogTitle    文章标题
     * @param commentId    评论ID
     * @param commentContent 评论内容
     * @param commenter    评论者名称
     * @return SaResult
     */
    SaResult sendTopLevelCommentApprovedNotification(String toEmail, String siteDomain,
                                                      Long blogId, String blogTitle, Long commentId,
                                                      String commentContent, String commenter);
}
