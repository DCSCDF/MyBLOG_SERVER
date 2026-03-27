/*
 * [SysConfigService.java]
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

package com.jiuliu.myblog_dev.service.config;

import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.config.ConfigCreateDTO;
import com.jiuliu.myblog_dev.dto.config.ConfigSystemKeysDTO;
import com.jiuliu.myblog_dev.dto.config.ConfigUpdateDTO;
import com.jiuliu.myblog_dev.dto.config.PageConfigCustomDTO;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public interface SysConfigService {

    /**
     * 系统默认配置项查询：仅支持系统内置项，按传入的 config_key 列表返回
     */
    SaResult getSystemConfigByKeys(ConfigSystemKeysDTO dto);

    /**
     * 用户自定义配置项分页列表：仅非系统内置项
     */
    SaResult getPageCustomConfigs(PageConfigCustomDTO pageDto);

    /**
     * 添加用户自定义配置项
     */
    SaResult createCustomConfig(ConfigCreateDTO dto);

    /**
     * 修改配置项：仅允许修改 config_value
     */
    SaResult updateConfigValue(ConfigUpdateDTO dto);

    /**
     * 删除非系统内置的配置项（按主键 id 删除）
     */
    SaResult deleteCustomConfig(Long id);

    /**
     * 公开查询配置项：根据传入的 config_key 列表返回配置值
     * 支持系统内置配置和用户自定义配置，无需登录即可访问
     *
     * @param keys 配置键列表
     * @return 配置项列表
     */
    SaResult getPublicConfigByKeys(@NotEmpty(message = "配置键列表不能为空") List<String> keys);

    /**
     * 获取网站基础信息
     * 包括：网站名称、网站域名、网站描述、备案号
     *
     * @return 网站基础信息
     */
    SaResult getSiteInfo();
}
