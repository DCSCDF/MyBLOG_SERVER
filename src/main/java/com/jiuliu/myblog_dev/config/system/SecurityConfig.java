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
 */

package com.jiuliu.myblog_dev.config.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.security.ActiveTokenRegistry;
import com.jiuliu.myblog_dev.security.BlogUserDetailsService;
import com.jiuliu.myblog_dev.security.JwtAuthenticationFilter;
import com.jiuliu.myblog_dev.security.JwtService;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.PrintWriter;
import java.util.List;

/**
 * Spring Security configuration: stateless JWT
 * authentication, URL rules for public endpoints, and method security
 * ({@code @PreAuthorize}) for the RBAC permission checks. 401/403 responses
 * keep the project's unified {@link Response} JSON shape.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        // Authorization is the canonical credential header; "token" is the
        // legacy header name kept for the existing frontend (see JwtAuthenticationFilter)
        config.setAllowedHeaders(List.of("Origin", "Content-Type", "Accept", "Authorization",
                "X-Requested-With", JwtAuthenticationFilter.LEGACY_TOKEN_HEADER));
        config.setExposedHeaders(List.of("Authorization", JwtAuthenticationFilter.LEGACY_TOKEN_HEADER));
        config.setAllowCredentials(true);
        config.setMaxAge(86400L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService,
                                                           BlogUserDetailsService userDetailsService,
                                                           ActiveTokenRegistry tokenRegistry) {
        return new JwtAuthenticationFilter(jwtService, userDetailsService, tokenRegistry);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                     JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {
                })
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public read APIs and captcha: no login required
                        .requestMatchers("/api/public/**", "/api/images/**", "/api/captcha/**",
                                "/favicon.ico", "/actuator/health", "/error").permitAll()
                        // Auth entry points: reachable anonymously, everything else
                        // under /api/auth requires an authenticated principal
                        .requestMatchers(HttpMethod.GET, "/api/auth/public-key").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/register", "/api/auth/register/code",
                                "/api/auth/register/confirm",
                                "/api/auth/find-password/code", "/api/auth/find-password/confirm",
                                "/api/auth/oauth/token").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) ->
                                writeJson(response, 401, ResponseUtil.fail("未授权，请先登录", 401)))
                        .accessDeniedHandler((request, response, e) ->
                                writeJson(response, 403, ResponseUtil.fail("没有权限", 403))))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeJson(HttpServletResponse response, int status, Response<Void> body) {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(objectMapper.writeValueAsString(body));
            writer.flush();
        } catch (Exception ignored) {
            // Response is already committed or closed; nothing else can be sent.
        }
    }
}
