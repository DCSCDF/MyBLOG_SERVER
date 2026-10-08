/*
 * [SecurityConfig.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/8 04:37
 */

package com.jiuliu.myblog_dev.config.system;

import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiuliu.myblog_dev.security.AuthTokenService;
import com.jiuliu.myblog_dev.security.BlogUserDetailsService;
import com.jiuliu.myblog_dev.security.TokenAuthenticationFilter;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

/**
 * Spring Security 配置。
 * 无状态令牌认证：请求头 token（app.security.token-name）承载不透明令牌，
 * 方法级授权由 @PreAuthorize 承担，权限/角色编码与原 ACL 表保持不变。
 * 401/403 均返回统一 JSON 格式（Response）。
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** 无需登录即可访问的接口（与迁移前的公开接口划分一致）。 */
    private static final String[] PUBLIC_PATHS = {
            "/api/auth/public-key",
            "/api/auth/login",
            "/api/auth/oauth/token",
            "/api/auth/register",
            "/api/auth/register/code",
            "/api/auth/register/confirm",
            "/api/auth/find-password/code",
            "/api/auth/find-password/confirm",
            // 登出由业务层自行判断登录状态并返回 401，保持原响应文案
            "/api/auth/logout",
            "/api/public/**",
            "/api/images/**",
            "/api/captcha/**",
            "/actuator/**",
            "/favicon.ico",
            "/error"
    };

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.security.token-name:token}")
    private String tokenName;

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthTokenService authTokenService,
                                                   BlogUserDetailsService userDetailsService) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJson(response, 401, "未授权，请先登录"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJson(response, 403, "没有权限")))
                .addFilterBefore(new TokenAuthenticationFilter(authTokenService, userDetailsService, tokenName),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        configuration.setAllowedHeaders(List.of("Origin", "Content-Type", "Accept", "Authorization",
                "X-Requested-With", tokenName));
        configuration.setExposedHeaders(List.of(tokenName));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(86400L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ResponseUtil.fail(message, status)));
        response.getWriter().flush();
    }
}
