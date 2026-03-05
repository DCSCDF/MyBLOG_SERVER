/*
 * [SmtpConnectionTester.java]
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

package com.jiuliu.myblog_dev.utils.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;


/**
 * SMTP 连接测试工具类
 * 用于在实际发送邮件前测试 SMTP 服务器连接是否正常
 */
@Component
public class SmtpConnectionTester {

    private static final Logger log = LoggerFactory.getLogger(SmtpConnectionTester.class);

    /**
     * 测试 SMTP 连接
     *
     * @param mailSender JavaMailSender 实例
     * @return ConnectionTestResult 测试结果
     */
    public ConnectionTestResult testConnection(JavaMailSenderImpl mailSender) {
        if (mailSender == null) {
            log.warn("SMTP 连接测试失败：JavaMailSender 为 null");
            return ConnectionTestResult.failed("邮件发送器未初始化");
        }

        String host = mailSender.getHost();
        int port = mailSender.getPort();
        String username = mailSender.getUsername();

        // 检查是否是禁用的发送器
        if ("disabled".equals(host) || port == 0) {
            log.warn("SMTP 连接测试失败：邮件发送器处于禁用状态");
            return ConnectionTestResult.failed("SMTP 配置未完成，请先在系统配置中完成 SMTP 相关配置");
        }

        log.info("开始测试 SMTP 连接：host={}, port={}, username={}", host, port, username);

        try {
            Session session = mailSender.getSession();
            Transport transport = session.getTransport("smtp");

            try {
                // 连接 SMTP 服务器
                transport.connect(host, port, username, mailSender.getPassword());
                log.info("SMTP 连接测试成功：host={}, port={}", host, port);
                return ConnectionTestResult.success();
            } catch (MessagingException e) {
                log.error("SMTP 连接测试失败：无法连接到 SMTP 服务器 - {}", e.getMessage());
                return parseConnectionError(e);
            } finally {
                // 关闭连接
                try {
                    if (transport != null && transport.isConnected()) {
                        transport.close();
                    }
                } catch (MessagingException e) {
                    log.debug("关闭 SMTP 连接时出错：{}", e.getMessage());
                }
            }
        } catch (MessagingException e) {
            log.error("SMTP 连接测试失败：获取 Transport 失败 - {}", e.getMessage());
            return ConnectionTestResult.failed("无法获取邮件传输对象：" + e.getMessage());
        } catch (Exception e) {
            log.error("SMTP 连接测试失败：未知异常 - {}", e.getMessage());
            return ConnectionTestResult.failed("连接测试异常：" + e.getMessage());
        }
    }

    /**
     * 解析连接错误信息
     */
    private ConnectionTestResult parseConnectionError(MessagingException e) {
        String errorMsg = e.getMessage();

        if (errorMsg == null) {
            return ConnectionTestResult.failed("SMTP 连接失败：未知错误");
        }

        // 认证失败
        if (errorMsg.contains("AuthenticationFailed") ||
                errorMsg.contains("authentication") ||
                errorMsg.contains("Authorization")) {
            return ConnectionTestResult.failed("SMTP 连接失败：用户名或密码错误");
        }

        // 连接拒绝
        if (errorMsg.contains("Connection refused") ||
                errorMsg.contains("Connect failed") ||
                errorMsg.contains("connect timed out")) {
            return ConnectionTestResult.failed("SMTP 连接失败：无法连接到服务器，请检查主机地址和端口");
        }

        // 超时
        if (errorMsg.contains("Timeout") ||
                errorMsg.contains("timed out") ||
                errorMsg.contains("Read timed out")) {
            return ConnectionTestResult.failed("SMTP 连接失败：连接超时，请检查网络或服务器配置");
        }

        // SSL/TLS 错误
        if (errorMsg.contains("SSL") ||
                errorMsg.contains("TLS") ||
                errorMsg.contains("ssl handshake failed") ||
                errorMsg.contains("sun.security.validator")) {
            return ConnectionTestResult.failed("SMTP 连接失败：SSL/TLS 握手失败，请检查 SSL 配置");
        }

        // 主机未知
        if (errorMsg.contains("UnknownHostException") ||
                errorMsg.contains("unknown host")) {
            return ConnectionTestResult.failed("SMTP 连接失败：无法解析主机名，请检查 SMTP 主机地址");
        }

        // 其他错误
        return ConnectionTestResult.failed("SMTP 连接失败：" + errorMsg);
    }

    /**
     * 连接测试结果
     */
    @Getter
    public static class ConnectionTestResult {
        private final boolean success;
        private final String message;

        private ConnectionTestResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static ConnectionTestResult success() {
            return new ConnectionTestResult(true, "SMTP 连接正常");
        }

        public static ConnectionTestResult failed(String message) {
            return new ConnectionTestResult(false, message);
        }

    }
}
