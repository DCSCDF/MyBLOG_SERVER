/*
 * [PublicCommentSecurityTest.java]
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

package com.jiuliu.myblog_dev.controller;

import com.jiuliu.myblog_dev.controller.pubilc.PublicCommentController;
import com.jiuliu.myblog_dev.dto.ServiceResult;
import com.jiuliu.myblog_dev.dto.comment.PublicCommentCreateDTO;
import com.jiuliu.myblog_dev.security.AuthenticatedUser;
import com.jiuliu.myblog_dev.service.comment.PublicCommentService;
import com.jiuliu.myblog_dev.utils.security.ClientIpUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 公共评论接口的登录态识别测试（由 SecurityUtils 替代 StpUtil 后）。
 * 匿名用户 isLogin=false；带管理权限的登录用户 isAdmin=true。
 */
class PublicCommentSecurityTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private PublicCommentController controller(PublicCommentService service) {
        ClientIpUtil clientIpUtil = mock(ClientIpUtil.class);
        when(clientIpUtil.getClientIp(any())).thenReturn("127.0.0.1");
        return new PublicCommentController(service, clientIpUtil);
    }

    private PublicCommentCreateDTO dto() {
        PublicCommentCreateDTO dto = new PublicCommentCreateDTO();
        dto.setBlogId(1L);
        dto.setParentId(0L);
        dto.setContent("hello");
        return dto;
    }

    @Test
    void anonymousCommentIsNotLoggedIn() {
        PublicCommentService service = mock(PublicCommentService.class);
        when(service.createComment(any(), any(), any(), eq(false), isNull(), eq(false)))
                .thenReturn(ServiceResult.data("ok"));
        controller(service).createComment(dto(), new MockHttpServletRequest());
        verify(service).createComment(any(), eq("127.0.0.1"), any(), eq(false), isNull(), eq(false));
    }

    @Test
    void loggedInAdminIsRecognised() {
        PublicCommentService service = mock(PublicCommentService.class);
        when(service.createComment(any(), any(), any(), eq(true), eq(7L), eq(true)))
                .thenReturn(ServiceResult.data("ok"));
        var principal = new AuthenticatedUser(7L, "root", "x", true,
                List.of(new SimpleGrantedAuthority("system:user:list")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "tok", principal.getAuthorities()));
        controller(service).createComment(dto(), new MockHttpServletRequest());
        verify(service).createComment(any(), eq("127.0.0.1"), any(), eq(true), eq(7L), eq(true));
    }
}
