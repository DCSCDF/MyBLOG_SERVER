package com.jiuliu.myblog_dev.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "test-only-secret-key-for-myblog-tests-0123456789";

    private ActiveTokenRegistry registry;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        registry = new ActiveTokenRegistry();
        jwtService = new JwtService(SECRET, "Myblog_dev", 7200, 2592000, registry);
    }

    @Test
    void generatedTokenParsesBackToSameUser() {
        String token = jwtService.generateToken(42L, "alice", false);

        JwtService.ParsedToken parsed = jwtService.parse(token);

        assertEquals(42L, parsed.userId());
        assertEquals("alice", parsed.username());
        assertTrue(registry.isLoggedIn(42L));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(42L, "alice", false);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThrows(JwtException.class, () -> jwtService.parse(tampered));
    }

    @Test
    void revokedTokenIsDeniedAndUserNoLongerLoggedIn() {
        String token = jwtService.generateToken(7L, "bob", false);
        String jti = jwtService.parse(token).jti();

        registry.revoke(jti);

        assertTrue(registry.isDenied(jti));
        assertFalse(registry.isLoggedIn(7L));
    }

    @Test
    void revokeAllForUserDeniesEveryTokenOfThatUserOnly() {
        String first = jwtService.generateToken(9L, "carol", false);
        jwtService.generateToken(9L, "carol", true);
        jwtService.generateToken(10L, "dave", false);

        registry.revokeAllForUser(9L);

        assertTrue(registry.isDenied(jwtService.parse(first).jti()));
        assertFalse(registry.isLoggedIn(9L));
        assertTrue(registry.isLoggedIn(10L));
    }

    @Test
    void rememberMeTokenLivesLongerThanDefaultToken() {
        String shortLived = jwtService.generateToken(1L, "eve", false);
        String longLived = jwtService.generateToken(1L, "eve", true);

        assertTrue(jwtService.parse(longLived).expiresAt()
                .after(jwtService.parse(shortLived).expiresAt()));
    }
}
