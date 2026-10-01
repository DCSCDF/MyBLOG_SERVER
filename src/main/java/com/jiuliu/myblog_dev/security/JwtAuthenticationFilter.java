package com.jiuliu.myblog_dev.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates requests from a JWT taken from {@code Authorization: Bearer}
 * (canonical) or the legacy {@code token} header (kept so the existing
 * frontend, which sends the {@code token} header name, keeps working unchanged).
 * Invalid/expired/revoked tokens leave the context anonymous: public
 * endpoints still proceed, protected ones are rejected by the entry point.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String LEGACY_TOKEN_HEADER = "token";

    private final JwtService jwtService;
    private final BlogUserDetailsService userDetailsService;
    private final ActiveTokenRegistry tokenRegistry;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   BlogUserDetailsService userDetailsService,
                                   ActiveTokenRegistry tokenRegistry) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.tokenRegistry = tokenRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                JwtService.ParsedToken parsed = jwtService.parse(token);
                if (!tokenRegistry.isDenied(parsed.jti())) {
                    BlogUserDetails user = userDetailsService.loadUserById(parsed.userId());
                    if (user != null && user.isEnabled()) {
                        var authentication = new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities());
                        // jti rides in details so logout can revoke this exact token
                        authentication.setDetails(parsed.jti());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    static String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length()).trim();
        }
        String legacy = request.getHeader(LEGACY_TOKEN_HEADER);
        return StringUtils.hasText(legacy) ? legacy.trim() : null;
    }
}
