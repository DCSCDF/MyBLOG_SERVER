package com.jiuliu.myblog_dev.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityUtilTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId, String jti, String... authorities) {
        List<org.springframework.security.core.GrantedAuthority> granted = java.util.Arrays.stream(authorities)
                .map(a -> (org.springframework.security.core.GrantedAuthority) new SimpleGrantedAuthority(a)).toList();
        BlogUserDetails principal = new BlogUserDetails(userId, "alice", "x", true, granted);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, granted);
        auth.setDetails(jti);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void exposesCurrentUserJtiAndAuthorities() {
        authenticate(42L, "jti-1", "article:create", "ROLE_USER");

        assertTrue(SecurityUtil.isAuthenticated());
        assertEquals(42L, SecurityUtil.getCurrentUserId());
        assertEquals(42L, SecurityUtil.getCurrentUserIdOrNull());
        assertEquals("jti-1", SecurityUtil.getCurrentJti());
        assertTrue(SecurityUtil.hasAuthority("article:create"));
        assertFalse(SecurityUtil.hasAuthority("article:delete"));
    }

    @Test
    void anonymousContextThrowsAndReportsUnauthenticated() {
        assertFalse(SecurityUtil.isAuthenticated());
        assertNull(SecurityUtil.getCurrentUserIdOrNull());
        assertNull(SecurityUtil.getCurrentJti());
        assertThrows(NotAuthenticatedException.class, SecurityUtil::getCurrentUserId);
    }
}
