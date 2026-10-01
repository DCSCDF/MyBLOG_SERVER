package com.jiuliu.myblog_dev.exception;

import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.security.BlogUserDetails;
import com.jiuliu.myblog_dev.security.NotAuthenticatedException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerAuthTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        return request;
    }

    @Test
    void notAuthenticatedMapsTo401() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        Response<Void> body = handler.handleNotAuthenticatedException(
                new NotAuthenticatedException("未授权，请先登录"), request(), response);

        assertEquals(401, response.getStatus());
        assertEquals(401, body.getCode());
        assertEquals(false, body.isSuccess());
    }

    @Test
    void accessDeniedForAnonymousMapsTo401Not403() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        Response<Void> body = handler.handleAccessDeniedException(
                new AccessDeniedException("denied"), request(), response);

        assertEquals(401, response.getStatus());
        assertEquals(401, body.getCode());
    }

    @Test
    void accessDeniedForAuthenticatedMapsTo403() {
        BlogUserDetails principal = new BlogUserDetails(1L, "alice", "x", true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        MockHttpServletResponse response = new MockHttpServletResponse();

        Response<Void> body = handler.handleAccessDeniedException(
                new AccessDeniedException("denied"), request(), response);

        assertEquals(403, response.getStatus());
        assertEquals(403, body.getCode());
        assertEquals("没有权限", body.getErrorMsg());
    }
}
