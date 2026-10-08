/*
 * [GlobalConfigController.java]
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

package com.jiuliu.myblog_dev.controller.pubilc;

import com.jiuliu.myblog_dev.dto.ServiceResult;

import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.config.ConfigItemResponseDTO;
import com.jiuliu.myblog_dev.dto.config.SiteInfoDTO;
import com.jiuliu.myblog_dev.service.config.SysConfigService;
import com.jiuliu.myblog_dev.utils.rateLimit.RateLimit;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公共配置接口 - 无需登录即可访问
 */
@RestController
@RequestMapping("/api/public/config")
public class GlobalConfigController {

    private final SysConfigService sysConfigService;

    public GlobalConfigController(SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    /**
     * 根据配置键获取配置值
     * POST /api/public/config
     * 请求体示例：{ "keys": ["site.name", "site.logo"] }
     */
    @RateLimit(count = 520, period = 1, prefix = "public_config_keys", ipBased = true)
    @PostMapping
    public Response<List<ConfigItemResponseDTO>> getConfigByKeys(@Valid @RequestBody ConfigKeysDTO dto) {
        ServiceResult serviceResult = sysConfigService.getPublicConfigByKeys(dto.getKeys());
        return handleServiceResult(serviceResult);
    }

    /**
     * 获取网站基础信息
     * GET /api/public/config/site-info
     * 无需登录，所有用户均可访问
     * 返回字段：
     * - siteName: 网站名称
     * - siteDomain: 网站域名
     * - siteDescription: 网站描述
     * - recordNumber: 备案号
     */
    @RateLimit(count = 500, period = 1, prefix = "public_config_site_info", ipBased = true)
    @GetMapping("/site-info")
    public Response<SiteInfoDTO> getSiteInfo() {
        ServiceResult serviceResult = sysConfigService.getSiteInfo();
        return handleServiceResult(serviceResult);
    }

    private <T> Response<T> handleServiceResult(ServiceResult serviceResult) {
        if (serviceResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            T data = (T) serviceResult.getData();
            return ResponseUtil.success(data, 200);
        }
        return ResponseUtil.fail(serviceResult.getMsg(), serviceResult.getCode());
    }

    /**
     * 配置键请求DTO
     */
    @Data
    public static class ConfigKeysDTO {
        @NotEmpty(message = "配置键列表不能为空")
        private List<String> keys;
    }
}
