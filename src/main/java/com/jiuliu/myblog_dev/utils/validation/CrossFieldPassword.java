/*
 * [CrossFieldPassword.java]
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

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 跨字段密码验证注解
 * <p>
 * 验证规则：
 * - 禁止密码与用户名相同
 * - 禁止密码包含用户名
 * <p>
 * 此注解用于类级别，验证两个字段之间的关系
 */
@Documented
@Constraint(validatedBy = CrossFieldPasswordValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CrossFieldPassword {

    String message() default "密码不能与用户名相同或包含用户名";

    String passwordField() default "password";

    String usernameField() default "username";

    @SuppressWarnings("unused")
    Class<?>[] groups() default {};

    @SuppressWarnings("unused")
    Class<? extends Payload>[] payload() default {};
}
