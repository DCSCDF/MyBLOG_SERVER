package com.jiuliu.myblog_dev.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Static accessors for the current principal:
 * {@link #getCurrentUserId()} returns the logged-in user's id,
 * {@link #isAuthenticated()} reports login state, and
 * {@link #hasAuthority(String)} checks a permission code.
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static boolean isAuthenticated() {
        Authentication auth = getAuthentication();
        return auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof BlogUserDetails;
    }

    /** Current user id, or throw 401 when anonymous. */
    public static Long getCurrentUserId() {
        BlogUserDetails principal = getPrincipalOrNull();
        if (principal == null) {
            throw new NotAuthenticatedException("未授权，请先登录");
        }
        return principal.getUserId();
    }

    public static Long getCurrentUserIdOrNull() {
        BlogUserDetails principal = getPrincipalOrNull();
        return principal == null ? null : principal.getUserId();
    }

    public static BlogUserDetails getPrincipalOrNull() {
        Authentication auth = getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof BlogUserDetails details) {
            return details;
        }
        return null;
    }

    /** JWT id of the current request's token (used to revoke it on logout). */
    public static String getCurrentJti() {
        Authentication auth = getAuthentication();
        if (auth != null && auth.getDetails() instanceof String jti) {
            return jti;
        }
        return null;
    }

    public static boolean hasAuthority(String authority) {
        Authentication auth = getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
