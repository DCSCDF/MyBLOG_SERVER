/*
 * [Disabled.java]
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

package com.jiuliu.myblog_dev.utils.disabled;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 禁用接口注解
 * 使用此注解标记的接口将被拦截并返回禁用提示
 * 使用示例：
 * <pre>
 * {@code
 * @Disabled
 * @GetMapping("/api/xxx")
 * public Response<?> someEndpoint() {
 *     return ResponseUtil.success();
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Disabled {

    /**
     * 禁用提示信息（可选）
     * 默认值由 DisabledException 提供
     */
    String message() default "";

    /**
     * HTTP 状态码（可选）
     * 默认返回 503 Service Unavailable
     */
    int code() default 503;
}
