/*
 * [MailConfig.java]
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

package com.jiuliu.myblog_dev.config;

import com.jiuliu.myblog_dev.mapper.config.SysConfigMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;


@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    private static final String KEY_SMTP_HOST = "smtp.host";
    private static final String KEY_SMTP_PORT = "smtp.port";
    private static final String KEY_SMTP_USERNAME = "smtp.username";
    private static final String KEY_SMTP_PASSWORD = "smtp.password";
    private static final String KEY_SMTP_FROM = "smtp.fromName";
    private static final String KEY_SMTP_SSL_ENABLED = "smtp.ssl.enabled";

    private final SysConfigMapper sysConfigMapper;


    private JavaMailSenderImpl mailSender;

    public MailConfig(SysConfigMapper sysConfigMapper) {
        this.sysConfigMapper = sysConfigMapper;
    }

    @PostConstruct
    public void init() {
        refreshMailSender();
        // 已移除定时刷新任务，改为在配置修改时手动触发刷新
    }

    @Bean
    public JavaMailSender javaMailSender() {
        if (mailSender == null) {
            refreshMailSender();
        }
        return mailSender;
    }

    public synchronized void refreshMailSender() {
        String host = getConfigValue(KEY_SMTP_HOST);
        String portStr = getConfigValue(KEY_SMTP_PORT);
        String username = getConfigValue(KEY_SMTP_USERNAME);
        String password = getConfigValue(KEY_SMTP_PASSWORD);
        String from = getConfigValue(KEY_SMTP_FROM);
        String sslEnabled = getConfigValue(KEY_SMTP_SSL_ENABLED);

        log.info("SMTP 配置刷新：host=[{}], port=[{}], username=[{}], from=[{}], ssl=[{}]",
                host, portStr, username, from, sslEnabled);

        try {
            if (host == null || host.isBlank() || "smtp.example.com".equals(host)) {
                log.warn("SMTP 配置未完成，请先在系统配置中修改 smtp.host 为实际的 SMTP 服务器地址");
                this.mailSender = createDisabledMailSender();
                return;
            }

            int port = 587;
            if (portStr != null && !portStr.isBlank()) {
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    log.warn("SMTP 端口配置无效，使用默认端口 587: {}", portStr);
                }
            }

            boolean ssl = "true".equalsIgnoreCase(sslEnabled);

            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(host);
            sender.setPort(port);
            sender.setUsername(username);
            sender.setPassword(password);
            sender.setDefaultEncoding("UTF-8");

            Properties properties = sender.getJavaMailProperties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.starttls.required", "true");
            if (ssl) {
                properties.put("mail.smtp.ssl.enable", "true");
                properties.put("mail.smtp.ssl.trust", host);
            }

            this.mailSender = sender;
            log.info("邮件发送器初始化成功，host={}, port={}, from={}", host, port, from);
        } catch (Exception e) {
            log.error("邮件发送器初始化失败: {}", e.getMessage(), e);
            this.mailSender = createDisabledMailSender();
        }
    }

    private JavaMailSenderImpl createDisabledMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("disabled");
        sender.setPort(0);
        return sender;
    }

    private String getConfigValue(String key) {
        try {
            return sysConfigMapper.selectValueByKey(key);
        } catch (Exception e) {
            log.warn("获取配置失败: {}: {}", key, e.getMessage());
        }
        return null;
    }

    public String getFromAddress() {
        return getConfigValue(KEY_SMTP_FROM);
    }

    public boolean isConfigured() {
        String host = getConfigValue(KEY_SMTP_HOST);
        log.info("SMTP 配置检查：host={}", host);
        boolean configured = host != null && !host.isBlank() && !"smtp.example.com".equals(host);
        if (configured) {
            refreshMailSender();
        }
        return configured;
    }
}
