/*
 * [RateLimit.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/1/19 17:26
 */

package com.jiuliu.myblog_dev.utils.rateLimit;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * 限流次数（默认 5 次）
     */
    int count() default 5;

    /**
     * 时间窗口，单位：秒（默认 2min）
     */
    int period() default 2;

    /**
     * 限流 key 前缀（可选，默认使用方法签名）
     */
    String prefix() default "rate_limit";
}