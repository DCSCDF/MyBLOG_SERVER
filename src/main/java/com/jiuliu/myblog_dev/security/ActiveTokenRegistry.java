package com.jiuliu.myblog_dev.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks issued JWT jtis so stateless tokens keep session semantics: logout revokes the current token, and password change /
 * account disable / account delete can force-logout every token of a user.
 * JWT signature validation itself stays stateless; this registry is only
 * consulted for revocation and the admin "is logged in" indicator.
 */
@Component
public class ActiveTokenRegistry {

    private record ActiveToken(Long userId, long expireAtMillis) {
    }

    private final ConcurrentHashMap<String, ActiveToken> active = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> deniedUntil = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Set<String>> jtisByUser = new ConcurrentHashMap<>();

    public void register(String jti, Long userId, long ttlSeconds) {
        active.put(jti, new ActiveToken(userId, System.currentTimeMillis() + ttlSeconds * 1000L));
        jtisByUser.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(jti);
    }

    public boolean isDenied(String jti) {
        if (jti == null) {
            return false;
        }
        Long until = deniedUntil.get(jti);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() > until) {
            deniedUntil.remove(jti, until);
            return false;
        }
        return true;
    }

    /** Revoke a single token (logout of the current session). */
    public void revoke(String jti) {
        if (jti == null) {
            return;
        }
        ActiveToken token = active.remove(jti);
        long until = token != null ? token.expireAtMillis()
                : System.currentTimeMillis() + 30L * 24 * 3600 * 1000;
        deniedUntil.put(jti, until);
        if (token != null) {
            removeFromUserIndex(token.userId(), jti);
        }
    }

    /** Revoke every token of a user (password change, disable, delete). */
    public void revokeAllForUser(Long userId) {
        if (userId == null) {
            return;
        }
        Set<String> jtis = jtisByUser.remove(userId);
        if (jtis == null) {
            return;
        }
        for (String jti : jtis) {
            ActiveToken token = active.remove(jti);
            if (token != null) {
                deniedUntil.put(jti, token.expireAtMillis());
            }
        }
    }

    /** Whether the user holds any live, non-revoked token. */
    public boolean isLoggedIn(Long userId) {
        Set<String> jtis = jtisByUser.get(userId);
        if (jtis == null || jtis.isEmpty()) {
            return false;
        }
        long now = System.currentTimeMillis();
        for (String jti : jtis) {
            ActiveToken token = active.get(jti);
            if (token != null && now <= token.expireAtMillis() && !isDenied(jti)) {
                return true;
            }
        }
        return false;
    }

    private void removeFromUserIndex(Long userId, String jti) {
        Set<String> set = jtisByUser.get(userId);
        if (set != null) {
            set.remove(jti);
            if (set.isEmpty()) {
                jtisByUser.remove(userId, set);
            }
        }
    }

    // Visible for tests
    Map<String, ?> activeView() {
        return active;
    }
}
