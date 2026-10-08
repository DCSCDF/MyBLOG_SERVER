/*
 * [AuthTempServicesTest.java]
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

package com.jiuliu.myblog_dev.utils.auth;

import com.jiuliu.myblog_dev.security.ExpiringValueStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 临时令牌/授权码/待验证信息服务的回归测试。
 * 这些服务原先基于 SaTokenDao，现基于 ExpiringValueStore，
 * 这里验证其一次性消费、过期键清理与失败计数等关键语义不变。
 */
class AuthTempServicesTest {

    @Test
    void tempLoginTokenIsSingleUse() {
        TempLoginTokenService service = new TempLoginTokenService(new ExpiringValueStore());
        String token = service.generateTempToken();
        assertThat(token).hasSize(32);
        assertThat(service.consumeToken(token)).isEqualTo("unbound");
        assertThat(service.consumeToken(token)).isNull();
        assertThat(service.consumeToken(null)).isNull();
        service.destroy();
    }

    @Test
    void oauthCodeExchangesOnceForToken() {
        OAuthCodeService service = new OAuthCodeService(new ExpiringValueStore());
        String code = service.generateCode(7L, "login-token-value");
        assertThat(code).hasSize(32);
        assertThat(service.consumeCodeAndGetToken(code)).isEqualTo("login-token-value");
        assertThat(service.consumeCodeAndGetToken(code)).isNull();
        assertThat(service.consumeCodeAndGetToken(null)).isNull();
        service.destroy();
    }

    @Test
    void registerPendingUserRoundTripAndAttemptCounting() {
        RegisterPendingUserService service = new RegisterPendingUserService(new ExpiringValueStore());
        service.savePendingUser("a@example.com", "alice", "encoded-pw", "123456", 5);
        RegisterPendingUserService.PendingUser pending = service.getPendingUser("a@example.com");
        assertThat(pending).isNotNull();
        assertThat(pending.getUsername()).isEqualTo("alice");
        assertThat(pending.getCode()).isEqualTo("123456");
        assertThat(service.recordFailedAttempt("a@example.com")).isEqualTo(1);
        assertThat(service.recordFailedAttempt("a@example.com")).isEqualTo(2);
        service.deletePendingUser("a@example.com");
        assertThat(service.getPendingUser("a@example.com")).isNull();
        assertThat(service.recordFailedAttempt("a@example.com")).isEqualTo(-1);
    }

    @Test
    void pendingPasswordResetAndRateLimit() {
        PendingPasswordResetService service = new PendingPasswordResetService(new ExpiringValueStore());
        assertThat(service.checkRateLimit("a@example.com", 5)).isZero();
        service.savePendingPasswordReset(7L, "alice", "a@example.com", "654321", 5);
        PendingPasswordResetService.PendingPasswordReset reset = service.getPendingPasswordReset("a@example.com");
        assertThat(reset).isNotNull();
        assertThat(reset.getUserId()).isEqualTo(7L);
        assertThat(reset.getCode()).isEqualTo("654321");
        assertThat(service.recordFailedAttempt("a@example.com")).isEqualTo(1);
        service.setRateLimit("a@example.com", 5);
        assertThat(service.checkRateLimit("a@example.com", 5)).isPositive();
        service.deletePendingPasswordReset("a@example.com");
        assertThat(service.getPendingPasswordReset("a@example.com")).isNull();
    }

    @Test
    void changeEmailPendingRoundTrip() {
        ChangeEmailPendingService service = new ChangeEmailPendingService(new ExpiringValueStore());
        service.savePendingEmailChange(7L, "new@example.com", "111222", 5);
        ChangeEmailPendingService.PendingEmailChange pending = service.getPendingEmailChange(7L);
        assertThat(pending).isNotNull();
        assertThat(pending.getNewEmail()).isEqualTo("new@example.com");
        assertThat(pending.getCode()).isEqualTo("111222");
        assertThat(service.recordFailedAttempt(7L)).isEqualTo(1);
        service.deletePendingEmailChange(7L);
        assertThat(service.getPendingEmailChange(7L)).isNull();
    }
}
