package com.jiuliu.myblog_dev.config.satoken;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Bean
    public SaTokenDao saTokenDao() {
        return new SaTokenDaoDefaultImpl(); // 内存实现
    }

}