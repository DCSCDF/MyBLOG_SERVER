/*
 * [ValidPassword.java]
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
 * 密码格式验证注解
 * <p>
 * 验证规则：
 * - 必须包含大写字母、小写字母、数字，三者缺一不可
 * - 最少 6 位，最多 32 位
 * - 禁止与用户名相同或包含用户名
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default "密码格式不正确";

    /**
     * 关联的用户名字段名（用于检查密码是否包含用户名）
     */
    @SuppressWarnings("unused")
    String usernameField() default "";

    @SuppressWarnings("unused")
    Class<?>[] groups() default {};
    
    @SuppressWarnings("unused")
    Class<? extends Payload>[] payload() default {};
}
