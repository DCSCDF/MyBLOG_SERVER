/*
 * [ConfigCreateDTO.java]
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

package com.jiuliu.myblog_dev.dto.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 添加用户自定义配置项
 */
@Data
public class ConfigCreateDTO {

    @NotBlank(message = "配置键不能为空")
    private String configKey;

    @NotBlank(message = "配置值不能为空")
    private String configValue;

    /**
     * 数据类型：string, boolean, integer, json, email, url, text，默认 string
     */
    private String dataType;

    /**
     * 校验规则，如 max_length=100, regex=...
     */
    private String validationRule;

    /**
     * 配置项说明，用于后台展示
     */
    private String description;
}
