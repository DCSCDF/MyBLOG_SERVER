/*
 * [UrlValidator.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/27
 */

package com.jiuliu.myblog_dev.utils.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.MalformedURLException;
import java.net.URL;

public class UrlValidator implements ConstraintValidator<ValidUrl, String> {

    private boolean requireHttps;

    @Override
    public void initialize(ValidUrl constraintAnnotation) {
        this.requireHttps = constraintAnnotation.requireHttps();
    }

    @Override
    public boolean isValid(String url, ConstraintValidatorContext context) {
        if (url == null || url.trim().isEmpty()) {
            return true;
        }

        try {
            URL parsedUrl = new URL(url.trim());
            String protocol = parsedUrl.getProtocol().toLowerCase();

            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                customizeMessage(context, "URL地址格式无效，请输入有效的网址");
                return false;
            }

            if (requireHttps && !"https".equals(protocol)) {
                customizeMessage(context, "URL地址必须使用HTTPS协议");
                return false;
            }

            if (parsedUrl.getHost() == null || parsedUrl.getHost().isEmpty()) {
                customizeMessage(context, "URL地址格式无效，请输入有效的网址");
                return false;
            }

            return true;
        } catch (MalformedURLException e) {
            customizeMessage(context, "URL地址格式无效，请输入有效的网址");
            return false;
        }
    }

    private void customizeMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}