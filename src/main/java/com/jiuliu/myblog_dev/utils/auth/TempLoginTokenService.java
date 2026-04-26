/*
 * [TempLoginTokenService.java]
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

package com.jiuliu.myblog_dev.utils.auth;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.util.SaFoxUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class TempLoginTokenService {

    private static final Logger log = LoggerFactory.getLogger(TempLoginTokenService.class);
    private static final String TOKEN_PREFIX = "temp_login_token:";
    private static final long EXPIRE_SECONDS = 60;
    private static final int MAX_LOCK_ENTRIES = 5000;
    private static final long LOCK_CLEANUP_INTERVAL_MINUTES = 10;

    private final SaTokenDao saTokenDao;

    // 每个 token 的锁（用于防止并发消费）
    private final ConcurrentHashMap<String, Object> tokenLocks = new ConcurrentHashMap<>();

    // 构造函数注入
    public TempLoginTokenService(SaTokenDao saTokenDao) {
        this.saTokenDao = saTokenDao;
        // 启动定时清理任务
        // 定时清理任务
        ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "token-lock-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupScheduler.scheduleAtFixedRate(this::cleanupStaleLocks,
                LOCK_CLEANUP_INTERVAL_MINUTES, LOCK_CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 生成一个未绑定用户的临时登录 Token
     * 前端先申请 token，后续在登录时携带该 token 完成认证
     * return 32位随机字符串 token
     */
    public String generateTempToken() {
        String token = SaFoxUtil.getRandomString(32);
        String key = TOKEN_PREFIX + token;
        saTokenDao.set(key, "unbound", EXPIRE_SECONDS);
        return token;
    }


//    /**
//     * 生成绑定用户的临时 Token
//     */
//    public String generateTempTokenForUser(Object loginId) {
//        if (loginId == null) {
//            throw new IllegalArgumentException("loginId 不能为空");
//        }
//        String token = SaFoxUtil.getRandomString(32);
//        String key = TOKEN_PREFIX + token;
//        saTokenDao.set(key, String.valueOf(loginId), EXPIRE_SECONDS);
//        return token;
//    }

    /**
     * 使用临时 Token（无论是否绑定用户）
     * 返回 value
     */

    public String consumeToken(String token) {
        if (token == null) return null;

        // 为每个 token 获取独立锁
        Object lock = tokenLocks.computeIfAbsent(token, k -> new Object());
        synchronized (lock) {
            try {
                String key = TOKEN_PREFIX + token;
                String value = saTokenDao.get(key);
                if (value != null) {
                    saTokenDao.delete(key);
                    return value;
                }
                return null; // 已被消费或过期
            } finally {
                // 清理锁
                tokenLocks.remove(token, lock);
            }
        }
    }

    /**
     * 清理长时间未使用的锁对象，防止内存泄漏
     * 注意：这是兜底机制，正常情况下锁会在 consumeToken 的 finally 块中被移除
     */
    private void cleanupStaleLocks() {
        int beforeSize = tokenLocks.size();
        if (beforeSize > MAX_LOCK_ENTRIES) {
            // 当锁数量超过阈值时，清除一半
            int targetSize = MAX_LOCK_ENTRIES / 2;
            int removed = 0;
            for (String key : tokenLocks.keySet()) {
                if (tokenLocks.size() <= targetSize) break;
                if (tokenLocks.remove(key) != null) {
                    removed++;
                }
            }
            log.warn("Token锁缓存清理完成，移除 {} 个过期锁，当前剩余: {}", removed, tokenLocks.size());
        }
    }


//    /**
//     * 验证令牌是否已使用或已过期
//     * 此方法用于检查一次性令牌（如登录临时令牌、操作确认令牌）的状态
//     *
//     * @param token 待验证的令牌字符串
//     * @return true 表示令牌已使用或已过期（不可用），false 表示令牌有效
//     */
//    public boolean isTokenUsedOrExpired(String token) {
//        if (token == null) return true;  // 空令牌视为无效
//
//        // 从持久化存储中获取令牌，如果不存在则说明已使用或已过期
//        return saTokenDao.get(TOKEN_PREFIX + token) == null;
//    }
//
//    /**
//     * 使令牌失效
//     * 立即从存储中删除令牌，使其不可再被使用
//     *
//     * @param token 需要失效的令牌字符串
//     */
//    public void invalidateToken(String token) {
//        if (token != null) {
//            // 从持久化存储中删除令牌
//            saTokenDao.delete(TOKEN_PREFIX + token);
//        }
//    }
//
//    /**
//     * 检查令牌是否存在
//     * 验证令牌是否在当前有效令牌集中
//     *
//     * @param token 需要检查的令牌字符串
//     * @return true 表示令牌存在（有效），false 表示令牌不存在
//     */
//    public boolean exists(String token) {
//        if (token == null) return false;  // 空令牌视为不存在
//
//        // 检查令牌是否存在于持久化存储中
//        return saTokenDao.get(TOKEN_PREFIX + token) != null;
//    }
}