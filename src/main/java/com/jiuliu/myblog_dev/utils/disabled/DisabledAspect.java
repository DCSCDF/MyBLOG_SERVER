/*
 * [DisabledAspect.java]
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

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 接口禁用切面
 * 拦截带有 @Disabled 注解的方法，直接抛出异常阻止执行
 */
@Aspect
@Component
public class DisabledAspect {

    private static final Logger log = LoggerFactory.getLogger(DisabledAspect.class);

    /**
     * 拦截所有带有 @Disabled 注解的方法
     */
    @Around("@annotation(disabled)")
    public Object handleDisabled(ProceedingJoinPoint joinPoint, Disabled disabled) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        String message = disabled.message();
        if (message == null || message.trim().isEmpty()) {
            message = "该接口已禁用";
        }

        log.warn("接口被禁用: [class={}, method={}, message={}]", className, methodName, message);

        throw new DisabledException(message, disabled.code());
    }
}
