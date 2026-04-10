/*
 * [CrossFieldPasswordValidator.java]
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

import java.lang.reflect.Field;

/**
 * 跨字段密码验证器
 * 用于检查密码是否与用户名相同或包含用户名
 * <p>
 * 注意：此验证器需要与 @CrossFieldPassword 注解配合使用
 * 通常在 DTO 类级别使用，验证两个字段之间的关系
 */
public class CrossFieldPasswordValidator implements ConstraintValidator<CrossFieldPassword, Object> {

    private String passwordField;
    private String usernameField;

    @Override
    public void initialize(CrossFieldPassword constraintAnnotation) {
        this.passwordField = constraintAnnotation.passwordField();
        this.usernameField = constraintAnnotation.usernameField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        try {
            Field passwordFieldRef = findField(value.getClass(), this.passwordField);
            Field usernameFieldRef = findField(value.getClass(), this.usernameField);

            if (passwordFieldRef == null || usernameFieldRef == null) {
                return true;
            }

            passwordFieldRef.setAccessible(true);
            usernameFieldRef.setAccessible(true);

            String password = (String) passwordFieldRef.get(value);
            String username = (String) usernameFieldRef.get(value);

            // 如果密码为空，跳过验证（由 @NotBlank 处理）
            if (password == null || password.isEmpty()) {
                return true;
            }

            // 如果用户名为空，跳过验证
            if (username == null || username.isEmpty()) {
                return true;
            }

            // 检查密码是否与用户名相同
            if (password.equalsIgnoreCase(username)) {
                customizeMessage(context, "密码不能与用户名相同");
                return false;
            }

            // 检查密码是否包含用户名
            if (password.toLowerCase().contains(username.toLowerCase())) {
                customizeMessage(context, "密码不能包含用户名");
                return false;
            }

        } catch (Exception e) {
            // 字段访问异常时跳过验证
            return true;
        }

        return true;
    }

    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private void customizeMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
