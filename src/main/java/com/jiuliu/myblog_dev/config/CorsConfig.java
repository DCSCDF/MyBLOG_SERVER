/*
 * [CorsConfig.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/1/21 01:26
 */

package com.jiuliu.myblog_dev.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Boot原生CORS配置
 * 使用WebMvcConfigurer实现全局跨域支持
 * 从配置文件读取允许的源
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Spring Boot 3.x 推荐使用 allowedOriginPatterns
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins)  // 注意：这里变了！
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                .allowedHeaders("Origin", "Content-Type", "Accept", "Authorization",
                        "X-Requested-With", "token", "DNT", "sec-ch-ua",
                        "sec-ch-ua-mobile", "sec-ch-ua-platform", "User-Agent")
                .exposedHeaders("X-Total-Count", "token")
                .allowCredentials(true)
                .maxAge(600);
    }
}