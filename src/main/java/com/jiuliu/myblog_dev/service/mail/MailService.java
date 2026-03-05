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
     * @return SaResult SaResult sendTest
     */
   SaResult sendTestMail(String to, String subject, String content);

    /**
     * 检查 SMTP 配置是否完成
     *
     * @return SaResult
     */
    SaResult checkSmtpConfiguration();
}
