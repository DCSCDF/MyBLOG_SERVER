/*
 * [MemoryStatus.java]
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

import org.springframework.lang.NonNull;

/**
 * 内存状态记录类（Java 17 record）
 * 自动生成 getter、equals、hashCode、toString 方法
 */
public record MemoryStatus(
        /*
          当前 JVM 已分配的总内存 (MB)
         */
        long totalMemoryMB,

        /*
          当前空闲内存 (MB)
         */
        long freeMemoryMB,

        /*
          JVM 最大可用内存 (MB) - 即 -Xmx 设置的值
         */
        long maxMemoryMB,

        /*
         * 已使用内存 (MB)
         */
        long usedMemoryMB,

        /*
         * 可用内存 (MB) - 包括空闲内存和未分配的内存
         */
        long availableMemoryMB,

        /*
         * 可用内存比例 (0.0 - 1.0)
         */
        double availableRatio,

        /*
         * 已使用内存比例 (0.0 - 1.0)
         */
        double usedRatio
) {

    /*
     * 判断是否处于内存警告状态（可用内存 < 15%）
     */
    public boolean isWarning() {
        return availableRatio < 0.15;
    }

    /*
     * 判断是否处于内存紧急状态（可用内存 < 10%）
     */
    public boolean isCritical() {
        return availableRatio < 0.10;
    }

    /*
     * 获取状态描述
     */
    public String getStatusLevel() {
        return isCritical() ? "CRITICAL" : (isWarning() ? "WARNING" : "NORMAL");
    }

    /*
     * 自定义 toString 格式
     */
    @NonNull
    @Override
    public String toString() {
        return String.format(
                "MemoryStatus[total=%dMB, free=%dMB, max=%dMB, used=%dMB, available=%dMB, " +
                        "availableRatio=%.2f%%, usedRatio=%.2f%%, status=%s]",
                totalMemoryMB, freeMemoryMB, maxMemoryMB, usedMemoryMB, availableMemoryMB,
                availableRatio * 100, usedRatio * 100,
                getStatusLevel()
        );
    }
}