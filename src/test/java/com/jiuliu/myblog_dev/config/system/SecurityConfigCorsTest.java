package com.jiuliu.myblog_dev.config.system;

import com.jiuliu.myblog_dev.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigCorsTest {

    @Test
    void corsSourceAllowsAuthHeadersAndCredentials() throws Exception {
        SecurityConfig config = new SecurityConfig();
        CorsConfiguration cors = config
                .corsConfigurationSource(new String[]{"http://localhost:5173"})
                .getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest());

        assertNotNull(cors);
        assertEquals(Boolean.TRUE, cors.getAllowCredentials());
        assertTrue(cors.getAllowedHeaders().contains("Authorization"));
        assertTrue(cors.getAllowedHeaders().contains(JwtAuthenticationFilter.LEGACY_TOKEN_HEADER));
        assertTrue(cors.getExposedHeaders().contains(JwtAuthenticationFilter.LEGACY_TOKEN_HEADER));
        assertTrue(cors.getAllowedMethods().containsAll(
                java.util.List.of("GET", "POST", "PUT", "DELETE", "PATCH")));
    }
}
