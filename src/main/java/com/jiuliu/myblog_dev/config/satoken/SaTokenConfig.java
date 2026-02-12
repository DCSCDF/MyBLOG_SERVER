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

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.util.SaResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Bean
    public SaTokenDao saTokenDao() {
        return new SaTokenDaoDefaultImpl(); // 内存实现
        // 如果使用 Redis，可以改为：
        // return new SaTokenDaoRedisImpl();
    }

    /**
     * Sa-Token 全局过滤器
     * 过滤器异常处理
     */
    @Bean
    public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()
                .addInclude("/**")
                .addExclude("/favicon.ico")
                // 移除了 setBeforeAuth 中的 CORS 处理
                .setAuth(obj -> {
                }) // 具体规则由注解处理
                .setError(e -> {
                    log.error("全局过滤器异常", e);
                    return SaResult.error("服务异常").setCode(500);
                });
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
}