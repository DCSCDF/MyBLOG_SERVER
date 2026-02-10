/*
 * [CorsFilterConfig.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/11 15:30
 */

package com.jiuliu.myblog_dev.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 自定义CORS过滤器，专门处理OPTIONS预检请求
 * 提供更灵活的跨域配置和预检缓存优化
 */
@Component
@Order(1) // 确保在其他过滤器之前执行
public class CorsFilterConfig implements Filter {

    private static final Logger log = LoggerFactory.getLogger(CorsFilterConfig.class);

    // 预检缓存时间：24小时（86400秒）
    private static final String PREFLIGHT_MAX_AGE = "86400";

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOriginsArray;

    private Set<String> allowedOrigins;

    @Override
    public void init(FilterConfig filterConfig) {
        // 初始化允许的源列表
        this.allowedOrigins = new HashSet<>(Arrays.asList(allowedOriginsArray));
        log.info("CORS过滤器初始化完成，允许的源: {}", Arrays.toString(allowedOriginsArray));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String origin = httpRequest.getHeader("Origin");
        String method = httpRequest.getMethod();

        // 设置基本的 CORS 响应头
        if (origin != null && isAllowedOrigin(origin)) {
            httpResponse.setHeader("Access-Control-Allow-Origin", origin);
        } else {
            httpResponse.setHeader("Access-Control-Allow-Origin", "*");
        }

        // 设置 CORS 相关响应头
        httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
        httpResponse.setHeader("Access-Control-Allow-Headers",
                "Content-Type, Authorization, token, X-Requested-With, Accept, Origin, X-CSRF-Token");
        httpResponse.setHeader("Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");
        httpResponse.setHeader("Access-Control-Expose-Headers",
                "token, Authorization, Content-Type, X-Total-Count");
        httpResponse.setHeader("Access-Control-Max-Age", PREFLIGHT_MAX_AGE);

        // 特别处理 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(method)) {
            log.debug("处理OPTIONS预检请求: Origin={}, Method={}", origin, method);

            // 对于预检请求，直接返回200 状态码，不继续执行后续过滤器链
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            httpResponse.getWriter().write("OK");
            httpResponse.getWriter().flush();
            return;
        }

        // 非 OPTIONS 请求继续执行过滤器链
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        log.info("CORS 过滤器销毁");
    }

    /**
     * 检查请求源是否在允许列表中
     */
    private boolean isAllowedOrigin(String origin) {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            return true; // 如果没有配置，则允许所有源
        }

        // 精确匹配
        if (allowedOrigins.contains(origin)) {
            return true;
        }

        // 通配符匹配（简单的域名匹配）
        for (String allowedOrigin : allowedOrigins) {
            if ("*".equals(allowedOrigin) ||
                    (allowedOrigin.startsWith("*") && origin.endsWith(allowedOrigin.substring(1))) ||
                    (allowedOrigin.endsWith("*") && origin.startsWith(allowedOrigin.substring(0, allowedOrigin.length() - 1)))) {
                return true;
            }
        }

        return false;
    }
}