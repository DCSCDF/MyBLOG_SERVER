package com.jiuliu.myblog_dev.utils.auth;

import com.jiuliu.myblog_dev.common.TtlStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the five scratch-store services migrated off the old token DAO onto
 * {@link TtlStore}: one-time consumption, round-trip serialization, delete,
 * failed-attempt counting, and password-reset rate limiting.
 */
class AuthScratchServicesTest {

    @Test
    void tempLoginTokenIsConsumedExactlyOnce() {
        TtlStore store = new TtlStore();
        TempLoginTokenService service = new TempLoginTokenService(store);
        try {
            String token = service.generateTempToken();
            assertNotNull(token);
            assertEquals("unbound", service.consumeToken(token));
            assertNull(service.consumeToken(token));
            assertNull(service.consumeToken(null));
            assertNull(service.consumeToken("never-issued"));
        } finally {
            service.destroy();
            store.destroy();
        }
    }

    @Test
    void oauthCodeExchangesForTokenExactlyOnce() {
        TtlStore store = new TtlStore();
        OAuthCodeService service = new OAuthCodeService(store);
        try {
            String code = service.generateCode(7L, "jwt-token-value");
            assertEquals("jwt-token-value", service.consumeCodeAndGetToken(code));
            assertNull(service.consumeCodeAndGetToken(code));
            assertNull(service.consumeCodeAndGetToken(null));
        } finally {
            service.destroy();
            store.destroy();
        }
    }

    @Test
    void registerPendingRoundTripsAttemptsAndDeletes() {
        TtlStore store = new TtlStore();
        RegisterPendingUserService service = new RegisterPendingUserService(store);
        try {
            service.savePendingUser("a@example.com", "alice", "encoded-pw", "123456", 5);

            RegisterPendingUserService.PendingUser pending = service.getPendingUser("a@example.com");
            assertNotNull(pending);
            assertEquals("alice", pending.getUsername());
            assertEquals("encoded-pw", pending.getPassword());
            assertEquals("123456", pending.getCode());

            assertEquals(1, service.recordFailedAttempt("a@example.com"));
            assertEquals(2, service.recordFailedAttempt("a@example.com"));
            assertEquals(-1, service.recordFailedAttempt("missing@example.com"));

            service.deletePendingUser("a@example.com");
            assertNull(service.getPendingUser("a@example.com"));
        } finally {
            store.destroy();
        }
    }

    @Test
    void passwordResetRoundTripsAndRateLimits() {
        TtlStore store = new TtlStore();
        PendingPasswordResetService service = new PendingPasswordResetService(store);
        try {
            assertEquals(0, service.checkRateLimit("a@example.com", 5));
            service.setRateLimit("a@example.com", 5);
            assertTrue(service.checkRateLimit("a@example.com", 5) > 0);

            service.savePendingPasswordReset(9L, "alice", "a@example.com", "654321", 5);
            PendingPasswordResetService.PendingPasswordReset pending =
                    service.getPendingPasswordReset("a@example.com");
            assertNotNull(pending);
            assertEquals(9L, pending.getUserId());
            assertEquals("654321", pending.getCode());
            assertEquals(1, service.recordFailedAttempt("a@example.com"));

            service.deletePendingPasswordReset("a@example.com");
            assertNull(service.getPendingPasswordReset("a@example.com"));
        } finally {
            store.destroy();
        }
    }

    @Test
    void changeEmailPendingRoundTripsAttemptsAndDeletes() {
        TtlStore store = new TtlStore();
        ChangeEmailPendingService service = new ChangeEmailPendingService(store);
        try {
            service.savePendingEmailChange(3L, "new@example.com", "111222", 5);

            ChangeEmailPendingService.PendingEmailChange pending = service.getPendingEmailChange(3L);
            assertNotNull(pending);
            assertEquals("new@example.com", pending.getNewEmail());
            assertEquals("111222", pending.getCode());
            assertEquals(1, service.recordFailedAttempt(3L));
            assertEquals(-1, service.recordFailedAttempt(404L));

            service.deletePendingEmailChange(3L);
            assertNull(service.getPendingEmailChange(3L));
        } finally {
            store.destroy();
        }
    }
}
