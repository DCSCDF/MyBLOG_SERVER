/*
 * [GlobalExceptionHandlerSecurityTest.java]
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

package com.jiuliu.myblog_dev.exception;

import com.jiuliu.myblog_dev.dto.Response;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 全局异常处理中 Spring Security 异常映射的回归测试：
 * 未登录统一 401，越权统一 403，响应体格式与迁移前一致。
 */
class GlobalExceptionHandlerSecurityTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void authenticationExceptionMapsToUnified401() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        Response<Void> body = handler.handleNotLoginException(
                new AuthenticationCredentialsNotFoundException("未授权，请先登录"),
                new MockHttpServletRequest(), response);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.getCode()).isEqualTo(401);
        assertThat(body.getErrorMsg()).isEqualTo("未授权，请先登录");
        assertThat(body.isSuccess()).isFalse();
    }

    @Test
    void accessDeniedExceptionMapsToUnified403() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        Response<Void> body = handler.handlePermissionDeniedException(
                new AccessDeniedException("denied"),
                new MockHttpServletRequest(), response);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.getCode()).isEqualTo(403);
        assertThat(body.getErrorMsg()).isEqualTo("没有权限");
    }
}
