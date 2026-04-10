/*
 * [UsernameValidator.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/4/10
 */

package com.jiuliu.myblog_dev.utils.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 用户名验证器实现
 */
public class UsernameValidator implements ConstraintValidator<Username, String> {

    // 系统保留词
    private static final Set<String> RESERVED_WORDS = new HashSet<>(Arrays.asList(
            "admin", "root", "system", "test", "administrator",
            "user", "guest", "super", "master", "owner",
            "moderator", "support", "help", "info", "webmaster",
            "null", "undefined", "true", "false"
    ));

    // 用户名正则：仅允许字母和数字，必须以字母开头，不能为纯数字
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]*[a-zA-Z0-9]$|^[a-zA-Z]$");

    @Override
    public void initialize(Username constraintAnnotation) {
        // 初始化操作
    }

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        if (username == null || username.isEmpty()) {
            // 不验证空值，空值应由 @NotBlank 处理
            return true;
        }

        // 检查长度：4-20 位
        if (username.length() < 4 || username.length() > 20) {
            customizeMessage(context, "用户名长度必须在 4-20 位之间");
            return false;
        }

        // 检查格式：仅允许字母和数字，必须以字母开头
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            customizeMessage(context, "用户名只能包含字母和数字，且必须以字母开头");
            return false;
        }

        // 检查是否为纯数字
        if (username.matches("^\\d+$")) {
            customizeMessage(context, "用户名不能为纯数字，必须包含至少一个字母");
            return false;
        }

        // 检查保留词
        String lowerUsername = username.toLowerCase();
        if (RESERVED_WORDS.contains(lowerUsername)) {
            customizeMessage(context, "该用户名已被系统保留，请选择其他用户名");
            return false;
        }

        return true;
    }

    /**
     * 自定义错误消息
     */
    private void customizeMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
