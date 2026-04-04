/*
 * [DynamicRateLimitService.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/4/1
 */

package com.jiuliu.myblog_dev.utils.rateLimit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 动态限流服务
 *
 * <p>基于滑动窗口算法实现，支持根据并发压力动态调整限流阈值。
 * 主要用于保护后端资源在高并发场景下不被击垮。</p>
 */
@Service
public class DynamicRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(DynamicRateLimitService.class);

    /**
     * 默认每分钟允许请求数（低负载）
     */
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 60;

    /**
     * 高负载时的最小限流数
     */
    private static final int MIN_REQUESTS_PER_MINUTE = 10;

    /**
     * 最大限流数
     */
    private static final int MAX_REQUESTS_PER_MINUTE = 200;

    /**
     * 当前活跃请求计数器
     */
    private final AtomicInteger activeRequests = new AtomicInteger(0);

    /**
     * 滑动窗口请求记录（每分钟一个桶）
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<Long, AtomicInteger>> windowMap = new ConcurrentHashMap<>();

    /**
     * 健康检查定时器
     */
    private ScheduledExecutorService healthCheckScheduler;

    /**
     * 限流器缓存
     */
    private final ConcurrentHashMap<String, RateLimiter> limiterCache = new ConcurrentHashMap<>();

    /**
     * 线程池用于清理过期数据
     */
    private ScheduledExecutorService cleanupScheduler;

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        healthCheckScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "health-check");
            t.setDaemon(true);
            return t;
        });
        healthCheckScheduler.scheduleAtFixedRate(this::adjustRateLimits, 10, 10, TimeUnit.SECONDS);

        cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "limiter-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredLimiters, 60, 60, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        if (healthCheckScheduler != null) {
            healthCheckScheduler.shutdown();
        }
        if (cleanupScheduler != null) {
            cleanupScheduler.shutdown();
        }
    }

    /**
     * 获取当前活跃请求数
     */
    public int getActiveRequestCount() {
        return activeRequests.get();
    }

    /**
     * 获取当前限流阈值
     */
    public int getCurrentRateLimit(String key) {
        RateLimiter limiter = limiterCache.get(key);
        if (limiter != null) {
            return limiter.getPermitsPerMinute();
        }
        return DEFAULT_REQUESTS_PER_MINUTE;
    }

    /**
     * 尝试获取令牌
     *
     * @param ip 客户端 IP
     * @param key 限流 key
     * @return true 表示允许请求，false 表示被限流
     */
    public boolean tryAcquire(String ip, String key) {
        String fullKey = key + ":" + ip;
        long currentMinute = System.currentTimeMillis() / 60000;

        ConcurrentHashMap<Long, AtomicInteger> window = windowMap.computeIfAbsent(fullKey,
                k -> new ConcurrentHashMap<>());

        AtomicInteger count = window.computeIfAbsent(currentMinute, k -> new AtomicInteger(0));

        int rateLimit = getEffectiveRateLimit();
        if (count.incrementAndGet() > rateLimit) {
            count.decrementAndGet();
            log.warn("IP [{}] 触发限流，当前阈值: {} req/min", ip, rateLimit);
            return false;
        }

        activeRequests.incrementAndGet();
        return true;
    }

    /**
     * 释放请求计数
     */
    public void release() {
        activeRequests.decrementAndGet();
    }

    /**
     * 获取有效的限流阈值（考虑当前活跃请求数）
     */
    private int getEffectiveRateLimit() {
        int active = activeRequests.get();
        int baseLimit = DEFAULT_REQUESTS_PER_MINUTE;

        if (active > 50) {
            baseLimit = Math.max(MIN_REQUESTS_PER_MINUTE, 60 - (active - 50));
        } else if (active > 30) {
            baseLimit = 60 - ((active - 30) * 2);
        }

        return Math.max(MIN_REQUESTS_PER_MINUTE, Math.min(baseLimit, MAX_REQUESTS_PER_MINUTE));
    }

    /**
     * 根据系统负载动态调整限流
     */
    private void adjustRateLimits() {
        int active = activeRequests.get();
        int currentLimit = getEffectiveRateLimit();

        log.debug("系统健康检查 - 活跃请求: {}, 当前限流阈值: {} req/min", active, currentLimit);
    }

    /**
     * 清理过期的限流器
     */
    private void cleanupExpiredLimiters() {
        long currentMinute = System.currentTimeMillis() / 60000;
        long expireThreshold = currentMinute - 5;

        windowMap.entrySet().removeIf(entry -> {
            entry.getValue().keySet().removeIf(minute -> minute < expireThreshold);
            return entry.getValue().isEmpty();
        });

        if (log.isDebugEnabled()) {
            log.debug("限流器清理完成，当前活跃 IP 数: {}", windowMap.size());
        }
    }

    /**
     * 获取系统负载状态
     */
    public LoadStatus getLoadStatus() {
        int active = activeRequests.get();
        int limit = getEffectiveRateLimit();

        if (active < 20) {
            return LoadStatus.LOW;
        } else if (active < 50) {
            return LoadStatus.NORMAL;
        } else if (active < 100) {
            return LoadStatus.HIGH;
        } else {
            return LoadStatus.CRITICAL;
        }
    }

    /**
     * 获取推荐的传输速度（KB/s）
     */
    public int getRecommendedTransferSpeed() {
        int active = activeRequests.get();

        if (active < 10) {
            return 1024;
        } else if (active < 30) {
            return 512;
        } else if (active < 50) {
            return 256;
        } else if (active < 80) {
            return 128;
        } else {
            return 64;
        }
    }

    /**
     * 限流器
     */
    public static class RateLimiter {
        private volatile int permitsPerMinute;

        public RateLimiter(int permitsPerMinute) {
            this.permitsPerMinute = permitsPerMinute;
        }

        public int getPermitsPerMinute() {
            return permitsPerMinute;
        }

        public void setPermitsPerMinute(int permitsPerMinute) {
            this.permitsPerMinute = permitsPerMinute;
        }
    }

    /**
     * 负载状态枚举
     */
    public enum LoadStatus {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }
}
