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
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 500;

    /**
     * 高负载时的最小限流数
     */
    private static final int MIN_REQUESTS_PER_MINUTE = 100;

    /**
     * 当前活跃请求计数器
     */
    private final AtomicInteger activeRequests = new AtomicInteger(0);

    /**
     * 健康检查定时器
     */
    private ScheduledExecutorService healthCheckScheduler;

    /**
     * 限流器缓存
     */
    @Getter
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
     * 活跃请求数 +1（由限流切面在放行请求时调用）
     */
    public void incrementActiveRequests() {
        activeRequests.incrementAndGet();
    }

    /**
     * 活跃请求数 -1（由限流切面在请求结束时调用）
     */
    public void decrementActiveRequests() {
        activeRequests.decrementAndGet();
    }

    /**
     * 获取有效限流阈值（考虑当前活跃请求数）
     */
    public int getEffectiveRateLimit() {
        int active = activeRequests.get();
        int baseLimit = DEFAULT_REQUESTS_PER_MINUTE;

        if (active > 100) {
            double factor = Math.max(0.2, 1.0 - (active - 100) / 100.0);
            baseLimit = (int) (DEFAULT_REQUESTS_PER_MINUTE * factor);
        } else if (active > 50) {
            double factor = Math.max(0.4, 1.0 - (active - 50) / 50.0);
            baseLimit = (int) (DEFAULT_REQUESTS_PER_MINUTE * factor);
        } else if (active > 30) {
            double factor = Math.max(0.6, 1.0 - (active - 30) / 20.0);
            baseLimit = (int) (DEFAULT_REQUESTS_PER_MINUTE * factor);
        }

        return Math.max(baseLimit, MIN_REQUESTS_PER_MINUTE);
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
        // limiterCache 目前没有过期机制，这里预留位置
        if (log.isDebugEnabled()) {
            log.debug("限流器清理检查完成，当前缓存数: {}", limiterCache.size());
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
    @Setter
    @Getter
    public static class RateLimiter {
        private volatile int permitsPerMinute;

        public RateLimiter(int permitsPerMinute) {
            this.permitsPerMinute = permitsPerMinute;
        }

    }

}
