/*
 * [ConfigSystemKeysDTO.java]
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

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 系统默认配置项查询请求：前端传入 config_key 数组，如 site.name, site.domain
 */
@Data
public class ConfigSystemKeysDTO {

    @NotEmpty(message = "配置键列表不能为空")
    private List<String> keys;
}
