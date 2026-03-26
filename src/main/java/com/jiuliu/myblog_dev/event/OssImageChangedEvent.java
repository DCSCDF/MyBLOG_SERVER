/*
 * [OssImageChangedEvent.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/26
 */

package com.jiuliu.myblog_dev.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * OSS图片变更事件
 * <p>用于在图片上传或删除时通知全局OSS列表刷新缓存</p>
 */
@Getter
public class OssImageChangedEvent extends ApplicationEvent {

    /**
     * 事件类型
     */
    private final EventType eventType;

    /**
     * 图片哈希值
     */
    private final String hash;

    /**
     * 用户ID（用于清除用户OSS列表缓存）
     */
    private final Long userId;

    public OssImageChangedEvent(Object source, EventType eventType, String hash, Long userId) {
        super(source);
        this.eventType = eventType;
        this.hash = hash;
        this.userId = userId;
    }

//    public OssImageChangedEvent(Object source, EventType eventType, String hash) {
//        this(source, eventType, hash, null);
//    }

    /**
     * 事件类型枚举
     */
    public enum EventType {
        UPLOAD,
        DELETE
    }
}
