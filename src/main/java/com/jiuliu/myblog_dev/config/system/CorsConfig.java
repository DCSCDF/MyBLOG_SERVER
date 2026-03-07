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

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    /**
     * 获取配置的 token 名称
     */
    @Getter
    @Value("${sa-token.token-name}")
    private String tokenName;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                // Origin - CORS 标准 header
                // Content-Type - 内容类型 header
                // Accept - 接受的内容类型 header
                // Authorization - 标准认证 header（保持兼容性）
                // X-Requested-With - AJAX 请求标识（可考虑后续删除）
                // tokenName - Sa-Token 自定义认证 header（来自配置）
                .allowedHeaders("Origin", "Content-Type", "Accept", "Authorization",
                        "X-Requested-With", tokenName)
                .exposedHeaders(tokenName)
                .allowCredentials(true)
                .maxAge(86400); // 24小时预检请求缓存
    }

}