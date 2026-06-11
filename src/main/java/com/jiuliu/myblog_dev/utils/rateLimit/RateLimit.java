/*
 * [RateLimit.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/18 11:52
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
     * 时间窗口内允许的请求次数（默认 500 次）
     */
    int count() default 500;

    /**
     * 时间窗口长度，单位：分钟（默认 1 分钟）
     */
    int period() default 1;

    /**
     * 限流 key 前缀（可选，默认使用方法签名）
     */
    String prefix() default "rate_limit";

    /**
     * 是否启用动态限流（根据系统负载自动调整）
     */
    boolean dynamic() default true;

    /**
     * 是否基于 IP 限流（默认 true），设为 false 则仅基于方法签名限流
     */
    boolean ipBased() default true;
}