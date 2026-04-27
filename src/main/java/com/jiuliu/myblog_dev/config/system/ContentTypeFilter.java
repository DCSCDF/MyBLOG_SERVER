/*
 * [ContentTypeFilter.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/4/27 07:30
 */

package com.jiuliu.myblog_dev.config.system;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 内容协商过滤器
 * 确保 API 请求始终能够返回 JSON 响应，避免 HttpMediaTypeNotAcceptableException
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContentTypeFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String acceptHeader = request.getHeader("Accept");

        // 如果 Accept 头不存在或者是 */*（接受任何类型），则设置为 application/json
        // 如果 Accept 头不包含 application/json，也需要添加
        if (acceptHeader == null || acceptHeader.isEmpty() || acceptHeader.contains("*/*")) {
            // 对于 API 路径，始终期望 JSON 响应
            request = new AcceptHeaderRequestWrapper(request, MediaType.APPLICATION_JSON_VALUE);
        } else if (!acceptHeader.contains("application/json")) {
            // 如果 Accept 不包含 json，在其后添加 json
            request = new AcceptHeaderRequestWrapper(request, acceptHeader + ",application/json");
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 只过滤 API 请求
        String path = request.getRequestURI();
        return !path.startsWith("/api/");
    }

    /**
     * 包装 HttpServletRequest，重写 getAcceptableHeaderTypes 方法
     */
    private static class AcceptHeaderRequestWrapper extends jakarta.servlet.http.HttpServletRequestWrapper {

        private final String acceptHeader;

        public AcceptHeaderRequestWrapper(HttpServletRequest request, String acceptHeader) {
            super(request);
            this.acceptHeader = acceptHeader;
        }

        @Override
        public String getHeader(String name) {
            if ("Accept".equalsIgnoreCase(name)) {
                return acceptHeader;
            }
            return super.getHeader(name);
        }
    }
}
