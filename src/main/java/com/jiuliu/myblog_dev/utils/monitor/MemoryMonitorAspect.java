/*
 * [MemoryMonitorAspect.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/6/11
 */

package com.jiuliu.myblog_dev.utils.monitor;

import com.jiuliu.myblog_dev.utils.security.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 内存监控切面，在关键接口调用前检查内存状态，防止 OOM 崩溃。
 * 主要监控验证码生成、图片处理等内存密集型操作。
 */
@Aspect
@Component
public class MemoryMonitorAspect {

    private static final Logger log = LoggerFactory.getLogger(MemoryMonitorAspect.class);

    /**
     * 内存警告阈值：当可用内存低于总内存的 15% 时触发警告
     */
    private static final double WARNING_THRESHOLD = 0.15;

    /**
     * 内存紧急阈值：当可用内存低于总内存的 10% 时拒绝请求
     */
    private static final double CRITICAL_THRESHOLD = 0.10;

    /**
     * 连续采样次数要求：连续 N 次达到紧急阈值才拒绝，避免 GC 波动误杀正常请求
     */
    private static final int CRITICAL_STREAK_REQUIRED = 2;

    private final ClientIpUtil clientIpUtil;

    private volatile int criticalStreak = 0;

    public MemoryMonitorAspect(ClientIpUtil clientIpUtil) {
        this.clientIpUtil = clientIpUtil;
    }

    /**
     * 监控验证码生成接口 - 内存密集型操作
     */
    @Around("execution(* com.jiuliu.myblog_dev.controller.user.auth.CaptchaController.gen(..))")
    public Object monitorCaptchaGeneration(ProceedingJoinPoint joinPoint) throws Throwable {
        return checkMemoryAndProceed(joinPoint, "验证码生成");
    }

    /**
     * 监控图片上传/处理接口 - 内存密集型操作
     */
    @Around("execution(* com.jiuliu.myblog_dev.controller.oss.OssController.*(..)) || " +
            "execution(* com.jiuliu.myblog_dev.service.oss.ImageService.*(..))")
    public Object monitorImageProcessing(ProceedingJoinPoint joinPoint) throws Throwable {
        return checkMemoryAndProceed(joinPoint, "图片处理");
    }

    /**
     * 监控博客创建/更新接口 - 可能涉及 Markdown 解析等内存操作
     */
    @Around("execution(* com.jiuliu.myblog_dev.controller.blog.BlogController.create(..)) || " +
            "execution(* com.jiuliu.myblog_dev.controller.blog.BlogController.update(..))")
    public Object monitorBlogOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        return checkMemoryAndProceed(joinPoint, "博客操作");
    }

    /**
     * 检查内存状态并决定是否继续执行
     */
    private Object checkMemoryAndProceed(ProceedingJoinPoint joinPoint, String operationName) throws Throwable {
        MemoryStatus status = getMemoryStatus();
        String methodSignature = getMethodSignature(joinPoint);

        // 记录内存状态（仅在 debug 级别）
        if (log.isDebugEnabled()) {
            log.debug("[{}] 内存状态: {} - 方法: {}", operationName, status, methodSignature);
        }

        // 检查是否处于紧急状态（连续采样达到阈值才拒绝，带滞回避免误杀）
        if (status.availableRatio() < CRITICAL_THRESHOLD) {
            criticalStreak++;
            if (criticalStreak >= CRITICAL_STREAK_REQUIRED) {
                log.error("[{}] 内存严重不足，拒绝请求! 状态: {} - 方法: {} - IP: {}",
                        operationName, status, methodSignature, getClientIp());
                throw new MemoryCriticalException(
                        "系统内存不足，暂时无法处理请求，请稍后重试。当前可用内存: " +
                                formatMemory(status.freeMemoryMB()) + "MB");
            }
            log.warn("[{}] 内存紧张(第{}次采样)，状态: {} - 方法: {} - IP: {}",
                    operationName, criticalStreak, status, methodSignature, getClientIp());
        } else {
            criticalStreak = 0;
        }

        // 检查是否需要警告
        if (status.availableRatio() < WARNING_THRESHOLD) {
            log.warn("[{}] 内存警告! 状态: {} - 方法: {} - IP: {}",
                    operationName, status, methodSignature, getClientIp());
        }

        return joinPoint.proceed();
    }

    /**
     * 获取当前内存状态
     */
    public static MemoryStatus getMemoryStatus() {
        Runtime runtime = Runtime.getRuntime();

        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;

        // 可用内存包括：当前空闲 + (最大可分配 - 当前已分配)
        long availableMemory = freeMemory + (maxMemory - totalMemory);

        double availableRatio = (double) availableMemory / maxMemory;
        double usedRatio = (double) usedMemory / maxMemory;

        return new MemoryStatus(
                bytesToMB(totalMemory),
                bytesToMB(freeMemory),
                bytesToMB(maxMemory),
                bytesToMB(usedMemory),
                bytesToMB(availableMemory),
                availableRatio,
                usedRatio
        );
    }

    /**
     * 字节转换为 MB
     */
    private static long bytesToMB(long bytes) {
        return bytes / (1024 * 1024);
    }

    /**
     * 格式化内存显示
     */
    private static String formatMemory(long mb) {
        return String.format("%.1f", (double) mb);
    }

    /**
     * 获取方法签名
     */
    private String getMethodSignature(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    /**
     * 获取客户端 IP（仅信任可信代理来源的转发头，防止伪造）
     */
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        return clientIpUtil.getClientIp(attributes.getRequest());
    }
}