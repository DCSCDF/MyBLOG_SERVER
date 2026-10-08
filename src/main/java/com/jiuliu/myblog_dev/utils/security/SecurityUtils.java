/*
 * [SecurityUtils.java]
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

package com.jiuliu.myblog_dev.utils.security;

import com.jiuliu.myblog_dev.security.AuthenticatedUser;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static boolean isLogin() {
        return getCurrentUserOrNull() != null;
    }

    /** 当前用户 ID；未登录时抛出 AuthenticationCredentialsNotFoundException（由全局处理转 401）。 */
    public static Long getCurrentUserId() {
        AuthenticatedUser user = getCurrentUserOrNull();
        if (user == null) {
            throw new AuthenticationCredentialsNotFoundException("未授权，请先登录");
        }
        return user.getId();
    }

    public static Long getCurrentUserIdOrNull() {
        AuthenticatedUser user = getCurrentUserOrNull();
        return user != null ? user.getId() : null;
    }

    public static AuthenticatedUser getCurrentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        return null;
    }

    /** 当前请求携带的登录令牌（登出当前会话时使用），未登录时为 null。 */
    public static String getCurrentToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof String token) {
            return token;
        }
        return null;
    }

    public static boolean hasPermission(String permissionCode) {
        return hasAuthority(permissionCode);
    }

    public static boolean hasRole(String roleCode) {
        return hasAuthority("ROLE_" + roleCode);
    }

    private static boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
            if (authority.equals(grantedAuthority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
