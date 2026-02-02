/*
 * [RateLimitAspect.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/1/19 17:52
 */

package com.jiuliu.myblog_dev.utils.rateLimit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    // 计数器和过期时间
    private final ConcurrentHashMap<String, AtomicInteger> counterMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> expireTimeMap = new ConcurrentHashMap<>();

    // 每个 key 的锁（避免全局锁）
    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    // 定时清理任务
    @SuppressWarnings("FieldCanBeLocal")
    private ScheduledExecutorService cleanupScheduler;

    @PostConstruct
    public void init() {
        // 启动后台清理任务：每30秒清理一次过期key
        cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limit-cleanup");
            t.setDaemon(true); // 随 JVM 退出
            return t;
        });
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredKeys, 30, 30, TimeUnit.SECONDS);
    }

    @Around("@annotation(rateLimit)")
    @SuppressWarnings("unused")
    public Object doRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("RateLimit 注解只能用于 Web 请求方法");
        }
        HttpServletRequest request = attributes.getRequest();

        String ip = getClientIpAddress(request);
        String limitKey = buildLimitKey(joinPoint, rateLimit, ip);
        long now = System.currentTimeMillis();
        long periodMin = (long) rateLimit.period() * 60 * 1000;

        log.debug("构建限流键: [key={}, ip={}, method={}]", limitKey, ip, getMethodSignature(joinPoint));

        AtomicInteger count = getOrCreateCounter(limitKey, now, periodMin);

        if (count.incrementAndGet() > rateLimit.count()) {
            log.warn("请求被限流: [ip={}, key={}, method={}]", ip, limitKey, getMethodSignature(joinPoint));
            throw new RateLimitException("请求过于频繁，请稍后再试");
        }

        return joinPoint.proceed();
    }

    private AtomicInteger getOrCreateCounter(String key, long now, long periodMs) {
        // 为每个 key 获取独立锁
        ReentrantLock lock = lockMap.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            Long expire = expireTimeMap.get(key);
            // 双重检查：是否已过期
            if (expire != null && now > expire) {
                log.debug("重置限流计数器: [key={}]", key);
                AtomicInteger newCounter = new AtomicInteger(0);
                counterMap.put(key, newCounter);
                expireTimeMap.put(key, now + periodMs);
                return newCounter;
            }

            AtomicInteger counter = counterMap.computeIfAbsent(key, k -> new AtomicInteger(0));
            expireTimeMap.putIfAbsent(key, now + periodMs);
            return counter;
        } finally {
            lock.unlock();
        }
    }

    private void cleanupExpiredKeys() {
        long now = System.currentTimeMillis();
        // 清理 expireTimeMap 中已过期的 key
        expireTimeMap.entrySet().removeIf(entry -> now > entry.getValue());
        // 同步清理 counterMap 和 lockMap（避免内存泄漏）
        counterMap.keySet().removeIf(key -> !expireTimeMap.containsKey(key));
        lockMap.keySet().removeIf(key -> !expireTimeMap.containsKey(key));
        if (log.isDebugEnabled()) {
            log.debug("限流缓存清理完成，当前活跃 key 数: {}", expireTimeMap.size());
        }
    }


    private String buildLimitKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit, String ip) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        return String.format("%s:%s:%s.%s",
                rateLimit.prefix(), ip, className, methodName);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            ip = ip.split(",")[0].trim();
            log.debug("通过 X-Forwarded-For 获取IP: [ip={}]", ip);
            return ip;
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            log.debug("通过 X-Real-IP 获取IP: [ip={}]", ip);
            return ip;
        }
        ip = request.getRemoteAddr();
        log.debug("获取IP: [ip={}]", ip);
        return ip;
    }

    private String getMethodSignature(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }
}