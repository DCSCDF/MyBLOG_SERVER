/*
 * [MemoryMonitorController.java]
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

package com.jiuliu.myblog_dev.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.utils.monitor.MemoryMonitorService;
import com.jiuliu.myblog_dev.utils.monitor.MemoryStatus;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 内存监控 API（仅管理员可访问）
 */
@RestController
@RequestMapping("/api/admin/monitor")
public class MemoryMonitorController {

    private final MemoryMonitorService memoryMonitorService;

    public MemoryMonitorController(MemoryMonitorService memoryMonitorService) {
        this.memoryMonitorService = memoryMonitorService;
    }

    /**
     * 获取当前内存状态
     */
    @GetMapping("/memory")
    @SaCheckRole("SUPER_ADMIN")
    public Response<Map<String, Object>> getMemoryStatus() {
        MemoryStatus status = memoryMonitorService.getCurrentStatus();

        Map<String, Object> data = new HashMap<>();
        data.put("totalMemoryMB", status.totalMemoryMB());
        data.put("freeMemoryMB", status.freeMemoryMB());
        data.put("maxMemoryMB", status.maxMemoryMB());
        data.put("usedMemoryMB", status.usedMemoryMB());
        data.put("availableMemoryMB", status.availableMemoryMB());
        data.put("availableRatio", status.availableRatio());
        data.put("usedRatio", status.usedRatio());
        data.put("status", status.getStatusLevel());

        return ResponseUtil.success(data, 200);
    }

    /**
     * 获取详细的内存报告（包含 Metaspace 等信息）
     */
    @GetMapping("/memory/detail")
    @SaCheckRole("SUPER_ADMIN")
    public Response<String> getMemoryDetail() {
        String report = memoryMonitorService.getDetailedReport();
        return ResponseUtil.success(report, 200);
    }
}