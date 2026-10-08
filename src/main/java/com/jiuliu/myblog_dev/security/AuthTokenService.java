/*
 * [AuthTokenService.java]
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

import com.jiuliu.myblog_dev.utils.security.RandomStringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录令牌服务（服务端不透明令牌会话管理）。
 * 保留原语义：不透明随机令牌（random-128 风格）、绝对过期
 * app.security.token-timeout（默认 30 天）、未记住我时的活跃超时（滑动续期）、
 * 单端登录（新登录挤掉旧令牌）。仅内存存储，重启后令牌失效，与原行为一致。
 */
@Service
public class AuthTokenService {

    private record Session(Long userId, long expiresAtMillis,
                           long activeTimeoutSeconds, long activeExpiresAtMillis) {
    }

    private final long tokenTimeoutSeconds;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> tokensByUser = new ConcurrentHashMap<>();

    public AuthTokenService(@Value("${app.security.token-timeout:2592000}") long tokenTimeoutSeconds) {
        this.tokenTimeoutSeconds = tokenTimeoutSeconds;
    }

    /** 登录并返回新令牌；activeTimeoutSeconds &lt;= 0 表示不启用活跃超时。 */
    public synchronized String login(Long userId, long activeTimeoutSeconds) {
        // 单端登录：新登录使该用户旧令牌失效
        logoutUser(userId);
        String token = RandomStringUtil.random(128);
        long now = System.currentTimeMillis();
        long expiresAt = now + tokenTimeoutSeconds * 1000L;
        long activeExpiresAt = activeTimeoutSeconds > 0 ? now + activeTimeoutSeconds * 1000L : -1L;
        sessions.put(token, new Session(userId, expiresAt, activeTimeoutSeconds, activeExpiresAt));
        tokensByUser.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(token);
        return token;
    }

    /** 校验令牌并返回用户 ID；校验通过时滑动续期活跃超时。 */
    public Optional<Long> resolveUserId(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Session session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        long now = System.currentTimeMillis();
        if (now > session.expiresAtMillis()
                || (session.activeExpiresAtMillis() > 0 && now > session.activeExpiresAtMillis())) {
            removeToken(token, session.userId());
            return Optional.empty();
        }
        if (session.activeExpiresAtMillis() > 0) {
            sessions.put(token, new Session(session.userId(), session.expiresAtMillis(),
                    session.activeTimeoutSeconds(), now + session.activeTimeoutSeconds() * 1000L));
        }
        return Optional.of(session.userId());
    }

    public synchronized void logout(String token) {
        if (token == null) {
            return;
        }
        Session session = sessions.remove(token);
        if (session != null) {
            removeFromUserIndex(token, session.userId());
        }
    }

    public synchronized void logoutUser(Long userId) {
        Set<String> tokens = tokensByUser.remove(userId);
        if (tokens != null) {
            tokens.forEach(sessions::remove);
        }
    }

    public boolean isLoggedIn(Long userId) {
        Set<String> tokens = tokensByUser.get(userId);
        if (tokens == null || tokens.isEmpty()) {
            return false;
        }
        // 任一令牌仍有效即视为在线（resolve 顺带清理过期令牌）
        for (String token : Set.copyOf(tokens)) {
            if (resolveUserId(token).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private void removeToken(String token, Long userId) {
        sessions.remove(token);
        removeFromUserIndex(token, userId);
    }

    private void removeFromUserIndex(String token, Long userId) {
        Set<String> tokens = tokensByUser.get(userId);
        if (tokens != null) {
            tokens.remove(token);
            if (tokens.isEmpty()) {
                tokensByUser.remove(userId, tokens);
            }
        }
    }
}
