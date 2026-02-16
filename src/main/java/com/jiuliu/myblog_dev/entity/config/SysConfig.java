/*
 * [SysConfig.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.entity.config;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_config")
public class SysConfig {

    @TableId("config_key")
    private String configKey;

    @TableField("config_value")
    private String configValue;

    @TableField("data_type")
    private String dataType;

    @TableField("validation_rule")
    private String validationRule;

    private String description;

    @TableField("is_system")
    private Integer isSystem;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
