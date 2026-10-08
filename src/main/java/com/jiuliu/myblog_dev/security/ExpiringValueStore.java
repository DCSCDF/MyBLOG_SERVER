/*
 * [ExpiringValueStore.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 */

package com.jiuliu.myblog_dev.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 带过期时间的内存键值存储。
 * 仅承载临时登录令牌、OAuth 授权码、邮箱验证码等短时数据：
 * 过期键读取为 null。后续可替换为 Redis 实现。
 */
@Component
public class ExpiringValueStore {

    private record Entry(String value, long expiresAtMillis) {
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public void set(String key, String value, long timeoutSeconds) {
        long expiresAt = timeoutSeconds < 0 ? Long.MAX_VALUE
                : System.currentTimeMillis() + timeoutSeconds * 1000L;
        store.put(key, new Entry(value, expiresAt));
    }

    public String get(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expiresAtMillis()) {
            store.remove(key, entry);
            return null;
        }
        return entry.value();
    }

    public void delete(String key) {
        store.remove(key);
    }
}
