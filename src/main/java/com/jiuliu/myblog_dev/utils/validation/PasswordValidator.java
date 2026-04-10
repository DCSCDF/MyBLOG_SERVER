/*
 * [PasswordValidator.java]
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

/**
 * 密码验证器实现
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // 初始化操作
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return true;
        }

        if (password.length() < 6 || password.length() > 32) {
            customizeMessage(context, "密码长度必须在 6-32 位之间");
            return false;
        }

        if (!password.matches(".*[A-Z].*")) {
            customizeMessage(context, "密码必须包含至少一个大写字母");
            return false;
        }

        if (!password.matches(".*[a-z].*")) {
            customizeMessage(context, "密码必须包含至少一个小写字母");
            return false;
        }

        if (!password.matches(".*\\d.*")) {
            customizeMessage(context, "密码必须包含至少一个数字");
            return false;
        }

        return true;
    }

    private void customizeMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
