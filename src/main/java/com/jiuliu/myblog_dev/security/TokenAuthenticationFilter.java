/*
 * [TokenAuthenticationFilter.java]
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

package com.jiuliu.myblog_dev.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 令牌认证过滤器（基于请求头不透明令牌）。
 * 从请求头（默认 token，可配置 app.security.token-name）读取不透明令牌，
 * 兼容 Authorization: Bearer 形式；校验通过后将 AuthenticatedUser
 * 放入 SecurityContext，令牌本身作为 credentials 保存以便登出时失效当前令牌。
 * 无令牌或令牌无效时不设置认证，由后续授权规则决定 401/放行。
 */
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final AuthTokenService authTokenService;
    private final BlogUserDetailsService userDetailsService;
    private final String tokenName;

    public TokenAuthenticationFilter(AuthTokenService authTokenService,
                                     BlogUserDetailsService userDetailsService,
                                     String tokenName) {
        this.authTokenService = authTokenService;
        this.userDetailsService = userDetailsService;
        this.tokenName = tokenName;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = resolveToken(request);
            if (StringUtils.hasText(token)) {
                authTokenService.resolveUserId(token).ifPresent(userId -> {
                    try {
                        AuthenticatedUser user = userDetailsService.loadUserById(userId);
                        if (user.isEnabled()) {
                            var authentication = new UsernamePasswordAuthenticationToken(
                                    user, token, user.getAuthorities());
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    } catch (UsernameNotFoundException ignored) {
                        // 用户已删除时视为未登录
                    }
                });
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String token = request.getHeader(tokenName);
        if (StringUtils.hasText(token)) {
            return token.trim();
        }
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return null;
    }
}
