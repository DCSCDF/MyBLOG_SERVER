/*
 * [MailServiceImpl.java]
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
import com.jiuliu.myblog_dev.config.business.MailConfig;
import com.jiuliu.myblog_dev.mapper.config.SysConfigMapper;
import com.jiuliu.myblog_dev.utils.mail.SmtpConnectionTester;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class MailServiceImpl implements MailService {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

    private static final String KEY_COMMENT_NOTIFICATION_ENABLED = "smtp.comment.enabled";

    private final MailConfig mailConfig;
    private final SmtpConnectionTester smtpConnectionTester;
    private final SysConfigMapper sysConfigMapper;

    public MailServiceImpl(MailConfig mailConfig, SmtpConnectionTester smtpConnectionTester,
            SysConfigMapper sysConfigMapper) {
        this.mailConfig = mailConfig;
        this.smtpConnectionTester = smtpConnectionTester;
        this.sysConfigMapper = sysConfigMapper;
    }

    @Override
    public SaResult sendTestMail(String to, String subject, String content) {
        if (!StringUtils.hasText(to)) {
            log.warn("发送测试邮件失败：收件人邮箱为空");
            return SaResult.error("收件人邮箱不能为空").setCode(400);
        }

        if (!StringUtils.hasText(subject)) {
            subject = "测试邮件";
        }

        if (!StringUtils.hasText(content)) {
            content = "这是一封来自博客系统的测试邮件。如果收到此邮件，说明邮件配置正确。";
        }

        if (!mailConfig.isConfigured()) {
            log.warn("发送测试邮件失败：SMTP 配置未完成");
            return SaResult.error("SMTP 配置未完成，请先在系统配置中完成 SMTP 相关配置").setCode(400);
        }

        // 在发送邮件前，先测试 SMTP 连接
        SaResult connectionTestResult = testSmtpConnection();
        if (connectionTestResult.getCode() != 200) {
            return connectionTestResult;
        }

        try {
            JavaMailSenderImpl mailSender = mailConfig.getMailSenderImpl();
            if (mailSender == null) {
                log.warn("发送测试邮件失败：邮件发送器未初始化");
                return SaResult.error("邮件发送器未初始化").setCode(500);
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String fromAddress = mailConfig.getFromAddress();
            if (!StringUtils.hasText(fromAddress)) {
                fromAddress = "noreply@localhost";
            }

            helper.setFrom(fromAddress != null ? fromAddress : "noreply@localhost");
            helper.setTo(to != null ? to : "");
            helper.setSubject(subject != null ? subject : "");
            helper.setText(content != null ? content : "", true);

            mailSender.send(message);

            log.info("测试邮件发送成功，to={}, subject={}", to, subject);
            return SaResult.ok("邮件发送成功");
        } catch (MessagingException e) {
            log.error("发送测试邮件失败：邮件消息构建异常，to={}, error={}", to, e.getMessage());
            return SaResult.error("邮件消息构建失败：" + e.getMessage()).setCode(500);
        } catch (MailException e) {
            log.error("发送测试邮件失败：邮件发送异常，to={}, error={}", to, e.getMessage());
            String errorMsg = e.getMessage();
            if (errorMsg != null) {
                if (errorMsg.contains("AuthenticationFailed")) {
                    return SaResult.error("邮件发送失败：用户名或密码错误，请检查 SMTP 用户名和密码配置").setCode(500);
                }
                if (errorMsg.contains("Connection refused") || errorMsg.contains("Connect failed")) {
                    return SaResult.error("邮件发送失败：无法连接到 SMTP 服务器，请检查 SMTP 主机和端口配置").setCode(500);
                }
                if (errorMsg.contains("Timeout") || errorMsg.contains("timed out")) {
                    return SaResult.error("邮件发送失败：连接 SMTP 服务器超时，请检查网络或 SMTP 主机配置").setCode(500);
                }
                if (errorMsg.contains("SSL") || errorMsg.contains("TLS")) {
                    return SaResult.error("邮件发送失败：SSL/TLS 连接失败，请检查 SSL 配置是否正确").setCode(500);
                }
                if (errorMsg.contains("502")) {
                    return SaResult.error("邮件发送失败：SMTP 服务器拒绝请求，可能是发件人邮箱格式不正确或 IP 被风控，请检查 SMTP 发件人配置").setCode(500);
                }
                if (errorMsg.contains("550")) {
                    return SaResult.error("邮件发送失败：SMTP 服务器拒绝，可能是收件人邮箱不存在或发件人邮箱未验证").setCode(500);
                }
                if (errorMsg.contains("553")) {
                    return SaResult.error("邮件发送失败：SMTP 服务器拒绝发件人邮箱，请检查 SMTP 发件人邮箱格式是否正确").setCode(500);
                }
                if (errorMsg.contains("Invalid input") || errorMsg.contains("501")) {
                    return SaResult.error("邮件发送失败：SMTP 参数格式错误，请检查发件人邮箱格式是否正确").setCode(500);
                }
                if (errorMsg.contains("must be same as authorization user")) {
                    return SaResult.error("邮件发送失败：发件人邮箱必须与 SMTP 用户名一致，请检查 SMTP 发件人配置是否与用户名相同").setCode(500);
                }
                return SaResult.error("邮件发送失败：" + errorMsg).setCode(500);
            }
            return SaResult.error("邮件发送失败：未知错误").setCode(500);
        } catch (Exception e) {
            log.error("发送测试邮件失败：未知异常，to={}, error={}", to, e.getMessage());
            return SaResult.error("邮件发送失败：" + e.getMessage()).setCode(500);
        }
    }

    @Override
    public SaResult checkSmtpConfiguration() {
        boolean configured = mailConfig.isConfigured();
        if (configured) {
            // 同时测试连接
            SaResult connectionTestResult = testSmtpConnection();
            if (connectionTestResult.getCode() == 200) {
                return SaResult.data("SMTP 配置已完成且连接正常");
            } else {
                return SaResult.error("SMTP 配置已完成，但无法连接到服务器：" + connectionTestResult.getMsg()).setCode(400);
            }
        } else {
            return SaResult.error("SMTP 配置未完成").setCode(400);
        }
    }

    /**
     * 测试 SMTP 连接
     *
     * @return SaResult 测试结果
     */
    private SaResult testSmtpConnection() {
        JavaMailSenderImpl mailSenderImpl = mailConfig.getMailSenderImpl();
        if (mailSenderImpl == null) {
            log.warn("SMTP 连接测试失败：无法获取 JavaMailSenderImpl 实例");
            return SaResult.error("邮件发送器未正确初始化").setCode(500);
        }

        SmtpConnectionTester.ConnectionTestResult result = smtpConnectionTester.testConnection(mailSenderImpl);
        if (!result.isSuccess()) {
            log.warn("SMTP 连接测试失败：{}", result.getMessage());
            return SaResult.error(result.getMessage()).setCode(500);
        }

        log.info("SMTP 连接测试通过");
        return SaResult.ok("SMTP 连接正常");
    }

    @Override
    public boolean isCommentNotificationEnabled() {
        String enabled = getConfigValue();
        return "true".equalsIgnoreCase(enabled);
    }

    @Override
    public SaResult sendCommentReviewNotification(String to, boolean approved, String siteDomain) {
        if (!StringUtils.hasText(to)) {
            log.warn("发送评论审核通知邮件失败：收件人邮箱为空");
            return SaResult.error("收件人邮箱为空").setCode(400);
        }

        if (!mailConfig.isConfigured()) {
            log.warn("发送评论审核通知邮件失败：SMTP 配置未完成");
            return SaResult.error("SMTP 配置未完成").setCode(400);
        }

        String subject = approved ? "您的评论已通过审核" : "您的评论未通过审核";
        String content = buildReviewNotificationContent(approved);

        return doSendMail(to, subject, content);
    }

    @Override
    public SaResult sendCommentReplyNotification(String to, String siteDomain, String replyContent) {
        if (!StringUtils.hasText(to)) {
            log.warn("发送评论回复通知邮件失败：收件人邮箱为空");
            return SaResult.error("收件人邮箱为空").setCode(400);
        }

        if (!mailConfig.isConfigured()) {
            log.warn("发送评论回复通知邮件失败：SMTP 配置未完成");
            return SaResult.error("SMTP 配置未完成").setCode(400);
        }

        String subject = "您的评论收到新的回复";
        String content = buildReplyNotificationContent(replyContent);

        return doSendMail(to, subject, content);
    }

    private String buildReviewNotificationContent(boolean approved) {
        return "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
                "<h2 style=\"color: #333;\">" + (approved ? "您的评论已通过审核" : "您的评论未通过审核") + "</h2>" +
                "<p style=\"color: #666; line-height: 1.6;\">" +
                "您好，</p>" +
                "<p style=\"color: #666; line-height: 1.6;\">" +
                (approved ? "您的评论已通过审核，感谢您的参与！" : "很抱歉，您的评论未通过审核，可能是因为内容不符合相关规定。") +
                "</p>" +
                // if (StringUtils.hasText(siteDomain)) {
                // sb.append("<p style=\"color: #666; line-height: 1.6;\">");
                // sb.append("查看详情：<a
                // href=\"").append(siteDomain).append("\">").append(siteDomain).append("</a>");
                // sb.append("</p>");
                // }
                "<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\">" +
                "<p style=\"color: #999; font-size: 12px;\">此邮件由系统自动发送，请勿回复。</p>" +
                "</div>";
    }

    private String buildReplyNotificationContent(String replyContent) {
        return "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
                "<h2 style=\"color: #333;\">您的评论收到新的回复</h2>" +
                "<p style=\"color: #666; line-height: 1.6;\">您好，</p>" +
                "<p style=\"color: #666; line-height: 1.6;\">您的评论有了新的回复：</p>" +
                "<blockquote style=\"background: #f5f5f5; padding: 15px; border-left: 4px solid #4CAF50; margin: 15px 0;\">"
                +
                "<p style=\"color: #333; margin: 0;\">" + escapeHtml(replyContent) + "</p>" +
                "</blockquote>" +
                // if (StringUtils.hasText(siteDomain)) {
                // sb.append("<p style=\"color: #666; line-height: 1.6;\">");
                // sb.append("查看详情：<a
                // href=\"").append(siteDomain).append("\">").append(siteDomain).append("</a>");
                // sb.append("</p>");
                // }
                "<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\">" +
                "<p style=\"color: #999; font-size: 12px;\">此邮件由系统自动发送，请勿回复。</p>" +
                "</div>";
    }

    @Override
    public SaResult sendNewCommentNotificationToAdmins(List<String> toEmailList, String siteDomain,
            boolean isGuest, Long commentId, Long blogId,
            String blogTitle, String commenter, String content) {
        if (toEmailList == null || toEmailList.isEmpty()) {
            log.warn("发送新评论通知邮件失败：收件人列表为空");
            return SaResult.error("收件人列表为空").setCode(400);
        }

        if (!mailConfig.isConfigured()) {
            log.warn("发送新评论通知邮件失败：SMTP 配置未完成");
            return SaResult.error("SMTP 配置未完成").setCode(400);
        }

        String subject = isGuest
                ? "【待审核】网站有新评论需要审核"
                : "网站有新评论";
        String htmlContent = buildNewCommentNotificationContent(isGuest, commentId, blogId,
                blogTitle, commenter, content);

        int successCount = 0;
        int failCount = 0;
        for (String toEmail : toEmailList) {
            if (!StringUtils.hasText(toEmail)) {
                continue;
            }
            SaResult result = doSendMail(toEmail, subject, htmlContent);
            if (result.getCode() == 200) {
                successCount++;
                log.info("新评论通知邮件发送成功，to={}, isGuest={}", toEmail, isGuest);
            } else {
                failCount++;
                log.warn("新评论通知邮件发送失败，to={}, error={}", toEmail, result.getMsg());
            }
        }

        if (successCount > 0) {
            return SaResult.ok("成功发送 " + successCount + " 封通知邮件" + (failCount > 0 ? "，" + failCount + " 封发送失败" : ""));
        } else {
            return SaResult.error("所有通知邮件发送失败").setCode(500);
        }
    }

    @Override
    public SaResult sendTopLevelCommentApprovedNotification(String toEmail, String siteDomain,
            Long blogId, String blogTitle, Long commentId,
            String commentContent, String commenter) {
        if (!StringUtils.hasText(toEmail)) {
            log.warn("发送顶级评论通过通知失败：收件人邮箱为空");
            return SaResult.error("收件人邮箱为空").setCode(400);
        }

        if (!mailConfig.isConfigured()) {
            log.warn("发送顶级评论通过通知失败：SMTP 配置未完成");
            return SaResult.error("SMTP 配置未完成").setCode(400);
        }

        String subject = "您的文章《" + blogTitle + "》有新评论";
        String htmlContent = buildTopLevelCommentApprovedContent(blogId, blogTitle,
                commentId, commentContent, commenter);

        return doSendMail(toEmail, subject, htmlContent);
    }

    @Override
    public SaResult sendRegisterVerificationCode(String toEmail, String code, String username) {
        if (!StringUtils.hasText(toEmail)) {
            log.warn("发送注册验证码邮件失败：收件人邮箱为空");
            return SaResult.error("收件人邮箱不能为空").setCode(400);
        }

        if (!mailConfig.isConfigured()) {
            log.warn("发送注册验证码邮件失败：SMTP 配置未完成");
            return SaResult.error("SMTP 配置未完成，请先在系统配置中完成 SMTP 相关配置").setCode(400);
        }

        String subject = "注册验证码";
        String htmlContent = buildRegisterVerificationCodeContent(username, code);

        return doSendMail(toEmail, subject, htmlContent);
    }

    @Override
    public SaResult sendChangeEmailVerificationCode(String toEmail, String code, String username) {
        if (!StringUtils.hasText(toEmail)) {
            log.warn("发送邮箱变更验证码邮件失败：收件人邮箱为空");
            return SaResult.error("收件人邮箱不能为空").setCode(400);
        }

        if (!mailConfig.isConfigured()) {
            log.warn("发送邮箱变更验证码邮件失败：SMTP 配置未完成");
            return SaResult.error("SMTP 配置未完成，请先在系统配置中完成 SMTP 相关配置").setCode(400);
        }

        String subject = "邮箱变更验证码";
        String htmlContent = buildChangeEmailVerificationCodeContent(username, code);

        return doSendMail(toEmail, subject, htmlContent);
    }

    @Override
    public SaResult sendFindPasswordVerificationCode(String toEmail, String code, String username) {
        if (!StringUtils.hasText(toEmail)) {
            log.warn("发送找回密码验证码邮件失败：收件人邮箱为空");
            return SaResult.error("收件人邮箱不能为空").setCode(400);
        }

        if (!mailConfig.isConfigured()) {
            log.warn("发送找回密码验证码邮件失败：SMTP 配置未完成");
            return SaResult.error("SMTP 配置未完成，请先在系统配置中完成 SMTP 相关配置").setCode(400);
        }

        String subject = "找回密码验证码";
        String htmlContent = buildFindPasswordVerificationCodeContent(username, code);

        return doSendMail(toEmail, subject, htmlContent);
    }

    private String buildRegisterVerificationCodeContent(String username, String code) {
        return "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
                "<h2 style=\"color: #333;\">邮箱验证码注册</h2>" +
                "<p style=\"color: #666; line-height: 1.6;\">您好 " + escapeHtml(username) + "，</p>" +
                "<p style=\"color: #666; line-height: 1.6;\">您的注册验证码为：</p>" +
                "<div style=\"background: #f5f5f5; padding: 20px; text-align: center; font-size: 32px; font-weight: bold; letter-spacing: 8px; margin: 20px 0;\">"
                +
                code +
                "</div>" +
                "<p style=\"color: #666; line-height: 1.6;\">验证码有效期为 5 分钟，请在有效期内完成注册。</p>" +
                "<p style=\"color: #999; line-height: 1.6;\">如果您没有发起注册请求，请忽略此邮件。</p>" +
                "<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\">" +
                "<p style=\"color: #999; font-size: 12px;\">此邮件由系统自动发送，请勿回复。</p>" +
                "</div>";
    }

    private String buildChangeEmailVerificationCodeContent(String username, String code) {
        return "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
                "<h2 style=\"color: #333;\">邮箱变更验证</h2>" +
                "<p style=\"color: #666; line-height: 1.6;\">您好 " + escapeHtml(username) + "，</p>" +
                "<p style=\"color: #666; line-height: 1.6;\">您正在更换邮箱，验证码为：</p>" +
                "<div style=\"background: #f5f5f5; padding: 20px; text-align: center; font-size: 32px; font-weight: bold; letter-spacing: 8px; margin: 20px 0;\">"
                +
                code +
                "</div>" +
                "<p style=\"color: #666; line-height: 1.6;\">验证码有效期为 5 分钟，请在有效期内完成邮箱更换。</p>" +
                "<p style=\"color: #999; line-height: 1.6;\">如果您没有发起更换邮箱请求，请忽略此邮件。</p>" +
                "<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\">" +
                "<p style=\"color: #999; font-size: 12px;\">此邮件由系统自动发送，请勿回复。</p>" +
                "</div>";
    }

    private String buildFindPasswordVerificationCodeContent(String username, String code) {
        return "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
                "<h2 style=\"color: #333;\">找回密码验证</h2>" +
                "<p style=\"color: #666; line-height: 1.6;\">您好 " + escapeHtml(username) + "，</p>" +
                "<p style=\"color: #666; line-height: 1.6;\">您正在找回密码，验证码为：</p>" +
                "<div style=\"background: #f5f5f5; padding: 20px; text-align: center; font-size: 32px; font-weight: bold; letter-spacing: 8px; margin: 20px 0;\">"
                +
                code +
                "</div>" +
                "<p style=\"color: #666; line-height: 1.6;\">验证码有效期为 5 分钟，请在有效期内完成密码重置。</p>" +
                "<p style=\"color: #999; line-height: 1.6;\">如果您没有发起找回密码请求，请忽略此邮件。</p>" +
                "<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\">" +
                "<p style=\"color: #999; font-size: 12px;\">此邮件由系统自动发送，请勿回复。</p>" +
                "</div>";
    }

    private String buildNewCommentNotificationContent(boolean isGuest, Long commentId,
            Long blogId, String blogTitle, String commenter, String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">");
        sb.append("<h2 style=\"color: #333;\">").append(isGuest ? "【待审核】网站有新评论需要处理" : "网站有新评论").append("</h2>");
        sb.append("<p style=\"color: #666; line-height: 1.6;\">您好，</p>");

        if (isGuest) {
            sb.append("<p style=\"color: #666; line-height: 1.6;\">有游客提交了新评论，需要您进行审核。</p>");
        } else {
            sb.append("<p style=\"color: #666; line-height: 1.6;\">有用户提交了新评论（已自动通过审核）。</p>");
        }

        sb.append("<div style=\"background: #f9f9f9; padding: 15px; border-radius: 5px; margin: 15px 0;\">");
        sb.append("<p style=\"color: #666; margin: 5px 0;\"><strong>文章ID：</strong>").append(blogId).append("</p>");
        sb.append("<p style=\"color: #666; margin: 5px 0;\"><strong>评论ID：</strong>").append(commentId).append("</p>");
        sb.append("<p style=\"color: #666; margin: 5px 0;\"><strong>文章：</strong>").append(escapeHtml(blogTitle))
                .append("</p>");
        sb.append("<p style=\"color: #666; margin: 5px 0;\"><strong>评论者：</strong>").append(escapeHtml(commenter))
                .append("</p>");
        sb.append("<p style=\"color: #666; margin: 5px 0;\"><strong>评论内容：</strong></p>");
        sb.append(
                "<blockquote style=\"background: #fff; padding: 10px; border-left: 3px solid #4CAF50; margin: 10px 0;\">");
        sb.append("<p style=\"color: #333; margin: 0;\">").append(escapeHtml(content)).append("</p>");
        sb.append("</blockquote>");
        sb.append("</div>");

        // if (isGuest) {
        // sb.append("<p style=\"color: #666; line-height: 1.6;\">请登录后台进行审核。</p>");
        // } else {
        // sb.append("<p style=\"color: #666; line-height: 1.6;\">查看详情。</p>");
        // }

        // if (StringUtils.hasText(siteDomain)) {
        // sb.append("<p style=\"color: #666; line-height: 1.6;\">");
        // sb.append("<a href=\"").append(siteDomain).append("/admin/comment\"
        // style=\"color: #4CAF50;\">前往评论管理</a>");
        // sb.append("</p>");
        // }

        sb.append("<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\">");
        sb.append("<p style=\"color: #999; font-size: 12px;\">此邮件由系统自动发送，请勿回复。</p>");
        sb.append("</div>");
        return sb.toString();
    }

    private String buildTopLevelCommentApprovedContent(Long blogId, String blogTitle,
            Long commentId, String commentContent, String commenter) {

        return "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
                "<h2 style=\"color: #333;\">您的文章有新评论</h2>" +
                "<p style=\"color: #666; line-height: 1.6;\">您好，</p>" +
                "<p style=\"color: #666; line-height: 1.6;\">您的文章《" + escapeHtml(blogTitle) + "》有新的评论了：</p>" +
                "<div style=\"background: #f9f9f9; padding: 15px; border-radius: 5px; margin: 15px 0;\">" +
                "<p style=\"color: #666; margin: 5px 0;\"><strong>文章ID：</strong>" + blogId + "</p>" +
                "<p style=\"color: #666; margin: 5px 0;\"><strong>评论ID：</strong>" + commentId + "</p>" +
                "<p style=\"color: #666; margin: 5px 0;\"><strong>评论者：</strong>" + escapeHtml(commenter) + "</p>" +
                "<p style=\"color: #666; margin: 5px 0;\"><strong>评论内容：</strong></p>" +
                "<blockquote style=\"background: #fff; padding: 10px; border-left: 3px solid #4CAF50; margin: 10px 0;\">"
                +
                "<p style=\"color: #333; margin: 0;\">" + escapeHtml(commentContent) + "</p>" +
                "</blockquote>" +
                "</div>" +

                // if (StringUtils.hasText(siteDomain)) {
                // sb.append("<p style=\"color: #666; line-height: 1.6;\">");
                // sb.append("查看详情：<a
                // href=\"").append(siteDomain).append("\">").append(siteDomain).append("</a>");
                // sb.append("</p>");
                // }

                "<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\">" +
                "<p style=\"color: #999; font-size: 12px;\">此邮件由系统自动发送，请勿回复。</p>" +
                "</div>";
    }

    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private SaResult doSendMail(String to, String subject, String content) {
        try {
            JavaMailSenderImpl mailSender = mailConfig.getMailSenderImpl();
            if (mailSender == null) {
                log.warn("发送邮件失败：邮件发送器未初始化");
                return SaResult.error("邮件发送器未初始化").setCode(500);
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String fromAddress = mailConfig.getFromAddress();
            if (!StringUtils.hasText(fromAddress)) {
                fromAddress = "noreply@localhost";
            }

            helper.setFrom(fromAddress != null ? fromAddress : "noreply@localhost");
            helper.setTo(to != null ? to : "");
            helper.setSubject(subject != null ? subject : "");
            helper.setText(content != null ? content : "", true);

            mailSender.send(message);

            log.info("邮件发送成功，to={}, subject={}", to, subject);
            return SaResult.ok("邮件发送成功");
        } catch (MessagingException e) {
            log.error("发送邮件失败：邮件消息构建异常，to={}, error={}", to, e.getMessage());
            return SaResult.error("邮件发送失败：" + e.getMessage()).setCode(500);
        } catch (MailException e) {
            log.error("发送邮件失败：邮件发送异常，to={}, error={}", to, e.getMessage());
            String errorMsg = e.getMessage();
            if (errorMsg != null) {
                if (errorMsg.contains("AuthenticationFailed")) {
                    return SaResult.error("邮件发送失败：用户名或密码错误").setCode(500);
                }
                if (errorMsg.contains("Connection refused") || errorMsg.contains("Connect failed")) {
                    return SaResult.error("邮件发送失败：无法连接到 SMTP 服务器").setCode(500);
                }
                if (errorMsg.contains("Timeout") || errorMsg.contains("timed out")) {
                    return SaResult.error("邮件发送失败：连接超时").setCode(500);
                }
                return SaResult.error("邮件发送失败：" + errorMsg).setCode(500);
            }
            return SaResult.error("邮件发送失败").setCode(500);
        } catch (Exception e) {
            log.error("发送邮件失败：未知异常，to={}, error={}", to, e.getMessage());
            return SaResult.error("邮件发送失败：" + e.getMessage()).setCode(500);
        }
    }

    private String getConfigValue() {
        try {
            return sysConfigMapper.selectValueByKey(MailServiceImpl.KEY_COMMENT_NOTIFICATION_ENABLED);
        } catch (Exception e) {
            log.warn("获取配置失败: {}: {}", MailServiceImpl.KEY_COMMENT_NOTIFICATION_ENABLED, e.getMessage());
        }
        return null;
    }
}
