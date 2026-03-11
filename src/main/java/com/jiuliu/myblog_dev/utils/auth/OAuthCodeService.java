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

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.util.SaFoxUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth 授权码服务
 * 用于生成和管理外部授权模式下的一次性授权码
 */
@Service
public class OAuthCodeService {

    private static final Logger log = LoggerFactory.getLogger(OAuthCodeService.class);

    private static final String CODE_PREFIX = "oauth_code:";
    private static final long CODE_EXPIRE_SECONDS = 300; // 5分钟

    private final SaTokenDao saTokenDao;
    private final ConcurrentHashMap<String, Object> codeLocks = new ConcurrentHashMap<>();

    public OAuthCodeService(SaTokenDao saTokenDao) {
        this.saTokenDao = saTokenDao;
    }

    /**
     * 生成一次性授权码
     *
     * @param userId 用户ID
     * @return 授权码
     */
    public String generateCode(Long userId) {
        String code = SaFoxUtil.getRandomString(32);
        String key = CODE_PREFIX + code;
        saTokenDao.set(key, String.valueOf(userId), CODE_EXPIRE_SECONDS);
        log.info("生成OAuth授权码，userId={}, code={}", userId, code);
        return code;
    }

    /**
     * 验证并消费授权码
     * 注意：此方法会删除授权码，使其只能使用一次
     *
     * @param code 授权码
     * @return 用户ID，如果授权码无效或已过期返回null
     */
    public Long consumeCode(String code) {
        if (code == null) {
            log.warn("OAuth授权码为空");
            return null;
        }

        Object lock = codeLocks.computeIfAbsent(code, k -> new Object());
        synchronized (lock) {
            try {
                String key = CODE_PREFIX + code;
                String userIdStr = saTokenDao.get(key);

                if (userIdStr == null) {
                    log.warn("OAuth授权码无效或已过期，code={}", code);
                    return null;
                }

                // 删除授权码，确保一次性使用
                saTokenDao.delete(key);
                log.info("OAuth授权码兑换成功，userId={}", userIdStr);

                return Long.parseLong(userIdStr);
            } catch (NumberFormatException e) {
                log.error("OAuth授权码解析用户ID失败，code={}", code, e);
                return null;
            } finally {
                codeLocks.remove(code, lock);
            }
        }
    }
    
}
