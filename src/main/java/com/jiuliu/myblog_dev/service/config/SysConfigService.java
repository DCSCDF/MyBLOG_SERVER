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
 * UpdateTime: 2026/2/22
 */

package com.jiuliu.myblog_dev.service.config;

import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.config.*;

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
}
