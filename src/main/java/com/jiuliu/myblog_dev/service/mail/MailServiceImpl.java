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
import com.jiuliu.myblog_dev.config.MailConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MailServiceImpl implements MailService {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final MailConfig mailConfig;

    public MailServiceImpl(JavaMailSender mailSender, MailConfig mailConfig) {
        this.mailSender = mailSender;
        this.mailConfig = mailConfig;
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

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String fromAddress = mailConfig.getFromAddress();
            if (!StringUtils.hasText(fromAddress)) {
                fromAddress = "noreply@localhost";
            }

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

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
            return SaResult.data("SMTP 配置已完成");
        } else {
            return SaResult.error("SMTP 配置未完成").setCode(400);
        }
    }
}
