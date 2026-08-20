/*
 * [MybatisMetaObjectHandler.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/8 04:37
 */

package com.jiuliu.myblog_dev.config.web;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 自动填充 createTime 和 updateTime
 * 实体类中使用 @TableField(fill = FieldFill.INSERT) 或 FieldFill.INSERT_UPDATE 时生效
 */
@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 强制覆盖 updateTime（不用 strictUpdateFill）
        // 原因：strictUpdateFill 只在字段为 null 时填充；而 updateById 携带旧值显式写回时
        // 会抑制 MySQL 的 ON UPDATE CURRENT_TIMESTAMP，导致 update_time 长期不刷新。
        this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
    }
}
