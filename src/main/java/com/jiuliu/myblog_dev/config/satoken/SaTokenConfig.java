/*
 * [SaTokenConfig.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/1/21 01:02
 */

package com.jiuliu.myblog_dev.config.satoken;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.util.SaResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOriginsArray;

    @Value("${sa-token.token-name}")
    private String tokenName;

    private Set<String> allowedOrigins;
    private Set<Pattern> allowedOriginPatterns;

    /**
     * 初始化允许的源集合
     */
    @PostConstruct
    public void initAllowedOrigins() {
        this.allowedOrigins = new HashSet<>(Arrays.asList(allowedOriginsArray));
        this.allowedOriginPatterns = new HashSet<>();
        
        // 预编译通配符模式为正则表达式
        for (String origin : allowedOriginsArray) {
            if (origin.contains("*")) {
                String regex = origin.replace(".", "\\.").replace("*", ".*");
                allowedOriginPatterns.add(Pattern.compile(regex));
            }
        }
        
        log.info("CORS 允许的源: {}", allowedOrigins);
        log.info("SaToken Token 名称: {}", tokenName);
    }

    @Bean
    public SaTokenDao saTokenDao() {
        return new SaTokenDaoDefaultImpl(); // 内存实现
        // 如果使用 Redis，可以改为：
        // return new SaTokenDaoRedisImpl();
    }

    /**
     * Sa-Token 全局过滤器
     * 处理 CORS 响应头、放行 OPTIONS 预检请求。
     * 不再处理具体的接口认证规则
     */
    @Bean
    public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()
                .addInclude("/**")
                .addExclude("/favicon.ico")
                .setBeforeAuth(this::handleCorsAndPreflight)
                // 此处 setAuth 为空，因为具体规则交给注解处理
                .setAuth(obj -> {
                })
                .setError(e -> {
                    log.error("全局过滤器异常", e);
                    return SaResult.error("服务异常").setCode(500);
                });
    }

    /**
     * 处理 CORS 及 OPTIONS 预检请求
     */
    private void handleCorsAndPreflight(Object obj) {
        SaResponse response = SaHolder.getResponse();
        String origin = SaHolder.getRequest().getHeader("Origin");
        String method = SaHolder.getRequest().getMethod();

        // 1. 处理源
        if (origin != null && isOriginAllowed(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }

        // 2. 设置CORS头
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD");
        response.setHeader("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization, X-Requested-With, " + tokenName + ", DNT, User-Agent");
        response.setHeader("Access-Control-Expose-Headers", tokenName + ", X-Total-Count");
        response.setHeader("Access-Control-Max-Age", "3600");

        // 3. 放行所有 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(method)) {
            response.setStatus(200);
            SaRouter.stop();
        }
    }

    /**
     * 注册注解拦截器
     * 职责：启用 Sa-Token 的注解鉴权功能。
     * 具体接口的权限规则，将在 Controller 中使用注解定义。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 启用注解鉴权，所有接口默认进入注解判断
        // 如果某个接口没有加注解，默认是放行的
        // 这样，权限规则就从全局配置转移到了具体的 Controller 方法上
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/**");
        // 这里不再需要 excludePathPatterns
        // 因为一个接口是否需要登录，由它自己头上的注解决定
    }

    /**
     * 检查请求源是否被允许
     */
    private boolean isOriginAllowed(String origin) {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            return false;
        }
        
        // 精确匹配
        if (allowedOrigins.contains(origin)) {
            return true;
        }
        
        // 通配符匹配
        if (allowedOrigins.contains("*")) {
            return true;
        }
        
        // 正则表达式匹配（处理通配符域名）
        for (Pattern pattern : allowedOriginPatterns) {
            if (pattern.matcher(origin).matches()) {
                return true;
            }
        }
        
        return false;
    }
}