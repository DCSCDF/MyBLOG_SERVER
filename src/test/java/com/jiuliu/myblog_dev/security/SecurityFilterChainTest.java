package com.jiuliu.myblog_dev.security;

import com.jiuliu.myblog_dev.config.system.SecurityConfig;
import com.jiuliu.myblog_dev.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the Spring Security migration at the HTTP layer with a minimal
 * standalone context (no DB): public URLs are anonymous, protected URLs
 * return 401 without a token, @PreAuthorize permission/role checks return
 * 403/200, both the canonical Bearer header and the legacy "token" header
 * authenticate, and a revoked (logged-out) token is rejected.
 */
@SpringBootTest(classes = SecurityFilterChainTest.TestConfig.class,
        properties = "app.cors.allowed-origins=http://localhost:5173")
@AutoConfigureMockMvc
class SecurityFilterChainTest {

    @RestController
    static class TestController {

        @GetMapping("/api/public/test/ping")
        public Map<String, Object> ping() {
            return Map.of("ok", true);
        }

        @PostMapping("/api/test/profile")
        public Map<String, Object> profile() {
            return Map.of("userId", SecurityUtil.getCurrentUserId());
        }

        @PostMapping("/api/test/blogs")
        @PreAuthorize("hasAuthority('article:create')")
        public Map<String, Object> createBlog() {
            return Map.of("created", true);
        }

        @GetMapping("/api/test/monitor")
        @PreAuthorize("hasRole('SUPER_ADMIN')")
        public Map<String, Object> monitor() {
            return Map.of("ok", true);
        }
    }

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class TestConfig {

        @Bean
        ActiveTokenRegistry tokenRegistry() {
            return new ActiveTokenRegistry();
        }

        @Bean
        JwtService jwtService(ActiveTokenRegistry tokenRegistry) {
            return new JwtService("test-only-secret-key-for-myblog-tests-0123456789",
                    "Myblog_dev", 3600, 3600, tokenRegistry);
        }

        @Bean
        BlogUserDetailsService userDetailsService() {
            return new BlogUserDetailsService(null, null) {
                @Override
                public BlogUserDetails loadUserById(Long userId) {
                    if (userId == 1L) {
                        return new BlogUserDetails(1L, "alice", "x", true,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("article:create")));
                    }
                    if (userId == 2L) {
                        return new BlogUserDetails(2L, "bob", "x", true,
                                List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    }
                    if (userId == 3L) {
                        return new BlogUserDetails(3L, "admin", "x", true,
                                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
                    }
                    if (userId == 4L) {
                        return new BlogUserDetails(4L, "disabled", "x", false,
                                List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    }
                    return null;
                }
            };
        }

        @Bean
        TestController testController() {
            return new TestController();
        }

        @Bean
        GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ActiveTokenRegistry tokenRegistry;

    @Test
    void publicEndpointAllowsAnonymous() throws Exception {
        mockMvc.perform(get("/api/public/test/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    void protectedEndpointRejectsAnonymousWith401() throws Exception {
        mockMvc.perform(post("/api/test/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void bearerTokenAuthenticates() throws Exception {
        String token = jwtService.generateToken(1L, "alice", false);

        mockMvc.perform(post("/api/test/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void legacyTokenHeaderAuthenticates() throws Exception {
        String token = jwtService.generateToken(1L, "alice", false);

        mockMvc.perform(post("/api/test/profile")
                        .header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void missingPermissionReturns403() throws Exception {
        String token = jwtService.generateToken(2L, "bob", false);

        mockMvc.perform(post("/api/test/blogs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void grantedPermissionAllows() throws Exception {
        String token = jwtService.generateToken(1L, "alice", false);

        mockMvc.perform(post("/api/test/blogs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(true));
    }

    @Test
    void roleCheckEnforced() throws Exception {
        String userToken = jwtService.generateToken(2L, "bob", false);
        String adminToken = jwtService.generateToken(3L, "admin", false);

        mockMvc.perform(get("/api/test/monitor")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/test/monitor")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void revokedTokenIsRejected() throws Exception {
        String token = jwtService.generateToken(1L, "alice", false);
        tokenRegistry.revoke(jwtService.parse(token).jti());

        mockMvc.perform(post("/api/test/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledUserTokenIsRejected() throws Exception {
        String token = jwtService.generateToken(4L, "disabled", false);

        mockMvc.perform(post("/api/test/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void garbageTokenOnProtectedEndpointIsRejected() throws Exception {
        mockMvc.perform(post("/api/test/profile")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void garbageTokenOnPublicEndpointStillAllowsAnonymous() throws Exception {
        mockMvc.perform(get("/api/public/test/ping")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isOk());
    }
}
