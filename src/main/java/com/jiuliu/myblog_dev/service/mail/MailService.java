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
}
