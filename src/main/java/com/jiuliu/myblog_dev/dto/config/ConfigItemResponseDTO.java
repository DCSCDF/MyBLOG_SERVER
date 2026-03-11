/*
 * [ConfigItemResponseDTO.java]
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

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConfigItemResponseDTO {

    /** 主键（系统默认配置查询可不返回；自定义配置列表返回供删除使用） */
    private Long id;
    private String configKey;
    private String configValue;
    private String dataType;
    private String validationRule;
    private String description;
    /** 是否公开：0=否，1=是（公开接口可查询） */
    private Integer isOpen;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
