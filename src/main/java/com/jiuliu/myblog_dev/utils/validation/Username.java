/*
 * [Username.java]
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
 * 用户名格式验证注解
 * <p>
 * 验证规则：
 * - 仅允许字母（a-z, A-Z）和数字（0-9）
 * - 最少 4 位，最多 20 位
 * - 必须以字母开头
 * - 不能为纯数字
 * - 禁止使用系统保留词（admin, root, system, test, administrator 等）
 */
@Documented
@Constraint(validatedBy = UsernameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Username {

    String message() default "用户名格式不正确";

    @SuppressWarnings("unused")
    Class<?>[] groups() default {};

    @SuppressWarnings("unused")
    Class<? extends Payload>[] payload() default {};
}
