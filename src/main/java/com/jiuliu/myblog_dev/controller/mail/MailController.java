/*
 * [MailController.java]
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

package com.jiuliu.myblog_dev.controller.mail;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.mail.SendTestMailDTO;
import com.jiuliu.myblog_dev.service.mail.MailService;
import com.jiuliu.myblog_dev.utils.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mail")
public class MailController {

    private final MailService mailService;

    public MailController(MailService mailService) {
        this.mailService = mailService;
    }

    /**
     * 发送测试邮件
     * 权限：system:config:edit
     */
    @SaCheckPermission("system:config:edit")
    @PostMapping("/test")
    public Response<Object> sendTestMail(@Valid @RequestBody SendTestMailDTO dto) {
        SaResult saResult = mailService.sendTestMail(dto.getTo(), dto.getSubject(), dto.getContent());
        return handleSaResult(saResult);
    }

    /**
     * 检查 SMTP 配置状态
     * 权限：system:config:edit
     */
    @SaCheckPermission("system:config:edit")
    @GetMapping("/status")
    public Response<Object> checkSmtpStatus() {
        SaResult saResult = mailService.checkSmtpConfiguration();
        return handleSaResult(saResult);
    }

    private <T> Response<T> handleSaResult(SaResult saResult) {
        if (saResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            T data = (T) saResult.getData();
            return ResponseUtil.success(data, 200);
        }
        return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
    }
}
