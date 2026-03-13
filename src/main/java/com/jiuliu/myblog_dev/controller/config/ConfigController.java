/*
 * [ConfigController.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/22
 */

package com.jiuliu.myblog_dev.controller.config;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.config.*;
import com.jiuliu.myblog_dev.service.config.SysConfigService;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final SysConfigService sysConfigService;

    public ConfigController(SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    /**
     * 系统默认配置项查询：前端传入 config_key 数组，仅返回系统内置项
     * 权限：config:system:list
     */
    @SaCheckPermission("system:config:systemlist")
    @PostMapping("/system/list")
    public Response<List<ConfigItemResponseDTO>> getSystemConfigByKeys(@Valid @RequestBody ConfigSystemKeysDTO dto) {
        SaResult saResult = sysConfigService.getSystemConfigByKeys(dto);
        return handleSaResult(saResult);
    }

    /**
     * 用户自定义配置项分页列表：仅非系统内置项
     * 权限：config:custom:list
     */
    @SaCheckPermission("system:config:customlist")
    @PostMapping("/custom/list")
    public Response<PageConfigCustomResponseDTO> getPageCustomConfigs(@Valid @RequestBody PageConfigCustomDTO pageDto) {
        SaResult saResult = sysConfigService.getPageCustomConfigs(pageDto);
        return handleSaResult(saResult);
    }

    /**
     * 添加用户自定义配置项
     * 权限：config:create
     */
    @SaCheckPermission("system:config:create")
    @PostMapping("/custom")
    public Response<ConfigItemResponseDTO> createCustomConfig(@Valid @RequestBody ConfigCreateDTO dto) {
        SaResult saResult = sysConfigService.createCustomConfig(dto);
        return handleSaResult(saResult);
    }

    /**
     * 修改网站配置项：仅允许修改 config_value
     * 权限：config:edit
     */
    @SaCheckPermission("system:config:edit")
    @PutMapping
    public Response<ConfigItemResponseDTO> updateConfig(@Valid @RequestBody ConfigUpdateDTO dto) {
        SaResult saResult = sysConfigService.updateConfigValue(dto);
        return handleSaResult(saResult);
    }

    /**
     * 删除非系统内置的配置项
     * 权限：config:delete
     */
    @SaCheckPermission("system:config:delete")
    @DeleteMapping("/custom/{id}")
    public Response<Object> deleteCustomConfig(@PathVariable Long id) {
        SaResult saResult = sysConfigService.deleteCustomConfig(id);
        return handleSaResult(saResult);
    }

    private <T> Response<T> handleSaResult(SaResult saResult) {
        if (saResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            T data = (T) saResult.getData();
            return ResponseUtil.success(data, 200);
        }
        return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
    }
}
