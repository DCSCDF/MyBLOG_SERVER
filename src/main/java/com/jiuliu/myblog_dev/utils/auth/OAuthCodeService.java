/*
 * [OAuthCodeService.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/11
 */

package com.jiuliu.myblog_dev.utils.auth;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.util.SaFoxUtil;
import jakarta.annotation.PreDestroy;

/**
 * OAuth 授权码服务
 * 用于生成和管理外部授权模式下的一次性授权码
 */
@Service
public class OAuthCodeService {

    private static final Logger log = LoggerFactory.getLogger(OAuthCodeService.class);

    private static final String CODE_PREFIX = "oauth_code:";
    private static final String TOKEN_KEY_SUFFIX = ":token";
    private static final long CODE_EXPIRE_SECONDS = 300; // 5分钟
    private static final int MAX_LOCK_ENTRIES = 5000;
    private static final long LOCK_CLEANUP_INTERVAL_MINUTES = 10;

    private final SaTokenDao saTokenDao;
    private final ConcurrentHashMap<String, Object> codeLocks = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleanupScheduler;

    public OAuthCodeService(SaTokenDao saTokenDao) {
        this.saTokenDao = saTokenDao;
        // 启动定时清理任务
        // 定时清理任务
        cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "oauth-code-lock-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupScheduler.scheduleAtFixedRate(this::cleanupStaleLocks,
                LOCK_CLEANUP_INTERVAL_MINUTES, LOCK_CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 清理长时间未使用的锁对象，防止内存泄漏
     * 注意：这是兜底机制，正常情况下锁会在 consumeCodeAndGetToken 的 finally 块中被移除
     */
    private void cleanupStaleLocks() {
        int beforeSize = codeLocks.size();
        if (beforeSize > MAX_LOCK_ENTRIES) {
            // 当锁数量超过阈值时，清除一半
            int targetSize = MAX_LOCK_ENTRIES / 2;
            int removed = 0;
            for (String key : codeLocks.keySet()) {
                if (codeLocks.size() <= targetSize) break;
                if (codeLocks.remove(key) != null) {
                    removed++;
                }
            }
            log.warn("OAuth Code锁缓存清理完成，移除 {} 个过期锁，当前剩余: {}", removed, codeLocks.size());
        }
    }

    @PreDestroy
    public void destroy() {
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 生成一次性授权码
     *
     * @param userId 用户ID
     * @param token  登录token
     * @return 授权码
     */
    public String generateCode(Long userId, String token) {
        String code = SaFoxUtil.getRandomString(32);
        String key = CODE_PREFIX + code;
        String tokenKey = key + TOKEN_KEY_SUFFIX;

        // 保存用户ID和token
        saTokenDao.set(key, String.valueOf(userId), CODE_EXPIRE_SECONDS);
        saTokenDao.set(tokenKey, token, CODE_EXPIRE_SECONDS);

        log.info("生成OAuth授权码，userId={}, code={}", userId, code);
        return code;
    }

    /**
     * 验证并消费授权码，同时返回登录token
     * 注意：此方法会删除授权码，使其只能使用一次
     *
     * @param code 授权码
     * @return token字符串，如果授权码无效或已过期返回null
     */
    public String consumeCodeAndGetToken(String code) {
        if (code == null) {
            log.warn("OAuth授权码为空");
            return null;
        }

        Object lock = codeLocks.computeIfAbsent(code, k -> new Object());
        synchronized (lock) {
            try {
                String key = CODE_PREFIX + code;
                String userIdStr = saTokenDao.get(key);
                String tokenKey = key + TOKEN_KEY_SUFFIX;
                String token = saTokenDao.get(tokenKey);

                if (userIdStr == null) {
                    log.warn("OAuth授权码无效或已过期，code={}", code);
                    return null;
                }

                // 删除授权码，确保一次性使用
                saTokenDao.delete(key);
                saTokenDao.delete(tokenKey);

                log.info("OAuth授权码兑换成功，userId={}", userIdStr);

                return token;
            } catch (Exception e) {
                log.error("OAuth授权码兑换失败，code={}", code, e);
                return null;
            } finally {
                codeLocks.remove(code, lock);
            }
        }
    }

}
