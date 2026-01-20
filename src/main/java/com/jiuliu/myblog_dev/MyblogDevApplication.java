package com.jiuliu.myblog_dev;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@MapperScan("com.jiuliu.myblog_dev.mapper")  // 启用 Mapper 扫描
@SpringBootApplication
@EnableAspectJAutoProxy  // 启用 AspectJ 代理支持AOP
public class MyblogDevApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyblogDevApplication.class, args);
    }
}
