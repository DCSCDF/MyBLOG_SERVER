/*
 * [CorsConfig.java]
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

import com.jiuliu.myblog_dev.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                // Authorization - 标准认证 header; "token" - 旧前端使用的认证 header（兼容保留）
                .allowedHeaders("Origin", "Content-Type", "Accept", "Authorization",
                        "X-Requested-With", JwtAuthenticationFilter.LEGACY_TOKEN_HEADER)
                .exposedHeaders("Authorization", JwtAuthenticationFilter.LEGACY_TOKEN_HEADER)
                .allowCredentials(true)
                .maxAge(86400); // 24小时预检请求缓存
    }

}