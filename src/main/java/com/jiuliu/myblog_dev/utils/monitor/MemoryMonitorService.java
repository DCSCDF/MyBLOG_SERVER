/*
 * [MemoryMonitorService.java]
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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 内存定时监控服务，定期记录内存状态并检测潜在的内存问题
 */
@Service
public class MemoryMonitorService {

    private static final Logger log = LoggerFactory.getLogger(MemoryMonitorService.class);

    /**
     * 监控间隔（秒）
     */
    private static final long MONITOR_INTERVAL_SECONDS = 30;

    /**
     * 内存警告阈值
     */
    private static final double WARNING_THRESHOLD = 0.20;

    /**
     * 内存紧急阈值
     */
    private static final double CRITICAL_THRESHOLD = 0.10;

    private ScheduledExecutorService monitorScheduler;
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    /**
     * 记录上次警告时间，避免频繁输出警告日志
     */
    private long lastWarningTime = 0;
    private long lastCriticalTime = 0;
    private static final long WARNING_COOLDOWN_MS = 60_000; // 1分钟
    private static final long CRITICAL_COOLDOWN_MS = 30_000; // 30秒

    @PostConstruct
    public void init() {
        monitorScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "memory-monitor");
            t.setDaemon(true);
            return t;
        });

        // 启动定时监控任务
        monitorScheduler.scheduleAtFixedRate(
                this::monitorMemory,
                MONITOR_INTERVAL_SECONDS,
                MONITOR_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );

        log.info("内存监控服务已启动，监控间隔: {}秒", MONITOR_INTERVAL_SECONDS);

        // 启动时记录一次初始状态
        logStartupMemoryInfo();
    }

    /**
     * 定时监控内存状态
     */
    private void monitorMemory() {
        try {
            MemoryStatus status = MemoryMonitorAspect.getMemoryStatus();

            // 检查堆内存使用情况
            MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
            long heapUsedMB = heapUsage.getUsed() / (1024 * 1024);
            long heapMaxMB = heapUsage.getMax() / (1024 * 1024);
            double heapUsedRatio = (double) heapUsage.getUsed() / heapUsage.getMax();

            // 检查非堆内存（Metaspace 等）
            MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
            long nonHeapUsedMB = nonHeapUsage.getUsed() / (1024 * 1024);

            // 正常状态下的定期记录
            if (status.availableRatio() >= WARNING_THRESHOLD) {
                if (log.isDebugEnabled()) {
                    log.debug("内存状态正常: {}", status);
                }
            }

            // 警告状态
            if (status.availableRatio() < WARNING_THRESHOLD && status.availableRatio() >= CRITICAL_THRESHOLD) {
                long now = System.currentTimeMillis();
                if (now - lastWarningTime > WARNING_COOLDOWN_MS) {
                    log.warn("内存警告! 堆内存使用率: {}% ({}/{}MB), 可用内存: {}MB, 可用比例: {}%",
                            (int) (heapUsedRatio * 100), heapUsedMB, heapMaxMB,
                            status.availableMemoryMB(),
                            (int) (status.availableRatio() * 100));
                    lastWarningTime = now;
                }
            }

            // 紧急状态
            if (status.availableRatio() < CRITICAL_THRESHOLD) {
                long now = System.currentTimeMillis();
                if (now - lastCriticalTime > CRITICAL_COOLDOWN_MS) {
                    log.error("内存严重不足! 堆内存使用率: {}% ({}/{}MB), Metaspace: {}MB, 可用内存: {}MB",
                            (int) (heapUsedRatio * 100), heapUsedMB, heapMaxMB,
                            nonHeapUsedMB, status.availableMemoryMB());
                    lastCriticalTime = now;

                    // 建议 GC（但不强制）
                    suggestGarbageCollection();
                }
            }

        } catch (Exception e) {
            log.error("内存监控异常: {}", e.getMessage());
        }
    }

    /**
     * 启动时记录内存配置信息
     */
    private void logStartupMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();

        log.info("JVM 内存配置");
        log.info("最大堆内存 (-Xmx): {}MB", runtime.maxMemory() / (1024 * 1024));
        log.info("初始堆内存 (-Xms): {}MB", runtime.totalMemory() / (1024 * 1024));

        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

        log.info("当前堆内存使用: {}MB / {}MB",
                heapUsage.getUsed() / (1024 * 1024),
                heapUsage.getMax() / (1024 * 1024));

        log.info("Metaspace 使用: {}MB (committed: {}MB)",
                nonHeapUsage.getUsed() / (1024 * 1024),
                nonHeapUsage.getCommitted() / (1024 * 1024));

        log.info("可用处理器: {}", runtime.availableProcessors());
    }

    /**
     * 建议 JVM 进行垃圾回收（仅建议，不强制）
     */
    private void suggestGarbageCollection() {
        log.info("建议 JVM 进行垃圾回收...");
        // explicit System.gc() removed: STW pause risk; rely on JVM args
    }

    /**
     * 手动获取当前内存状态（供外部调用）
     */
    public MemoryStatus getCurrentStatus() {
        return MemoryMonitorAspect.getMemoryStatus();
    }

    /**
     * 获取详细的内存使用报告
     */
    public String getDetailedReport() {
        MemoryStatus status = getCurrentStatus();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

        return "\n*内存详细报告:\n" +
                String.format("堆内存: 已用 %dMB / 已分配 %dMB / 最大 %dMB\n",
                        heapUsage.getUsed() / (1024 * 1024),
                        heapUsage.getCommitted() / (1024 * 1024),
                        heapUsage.getMax() / (1024 * 1024)) +
                String.format("非堆内存 (Metaspace等): 已用 %dMB / 已分配 %dMB\n",
                        nonHeapUsage.getUsed() / (1024 * 1024),
                        nonHeapUsage.getCommitted() / (1024 * 1024)) +
                String.format("可用内存: %dMB (%.1f%%)\n",
                        status.availableMemoryMB(),
                        status.availableRatio() * 100) +
                String.format("状态: %s\n",
                        status.isCritical() ? "CRITICAL" :
                                (status.isWarning() ? "WARNING" : "NORMAL"));
    }

    @PreDestroy
    public void destroy() {
        if (monitorScheduler != null) {
            monitorScheduler.shutdown();
            try {
                if (!monitorScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    monitorScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                monitorScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("内存监控服务已停止");
        }
    }
}