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

import org.springframework.security.access.prepost.PreAuthorize;
import com.jiuliu.myblog_dev.common.ServiceResult;
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
    @PreAuthorize("hasAuthority('system:config:systemlist')")
    @PostMapping("/system/list")
    public Response<List<ConfigItemResponseDTO>> getSystemConfigByKeys(@Valid @RequestBody ConfigSystemKeysDTO dto) {
        ServiceResult serviceResult = sysConfigService.getSystemConfigByKeys(dto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 用户自定义配置项分页列表：仅非系统内置项
     * 权限：config:custom:list
     */
    @PreAuthorize("hasAuthority('system:config:customlist')")
    @PostMapping("/custom/list")
    public Response<PageConfigCustomResponseDTO> getPageCustomConfigs(@Valid @RequestBody PageConfigCustomDTO pageDto) {
        ServiceResult serviceResult = sysConfigService.getPageCustomConfigs(pageDto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 添加用户自定义配置项
     * 权限：config:create
     */
    @PreAuthorize("hasAuthority('system:config:create')")
    @PostMapping("/custom")
    public Response<ConfigItemResponseDTO> createCustomConfig(@Valid @RequestBody ConfigCreateDTO dto) {
        ServiceResult serviceResult = sysConfigService.createCustomConfig(dto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 修改网站配置项：仅允许修改 config_value
     * 权限：config:edit
     */
    @PreAuthorize("hasAuthority('system:config:edit')")
    @PutMapping
    public Response<ConfigItemResponseDTO> updateConfig(@Valid @RequestBody ConfigUpdateDTO dto) {
        ServiceResult serviceResult = sysConfigService.updateConfigValue(dto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 删除非系统内置的配置项
     * 权限：config:delete
     */
    @PreAuthorize("hasAuthority('system:config:delete')")
    @DeleteMapping("/custom/{id}")
    public Response<Object> deleteCustomConfig(@PathVariable Long id) {
        ServiceResult serviceResult = sysConfigService.deleteCustomConfig(id);
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
}
