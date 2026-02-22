/*
 * [MyblogDevApplication.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/18 11:52
 */

package com.jiuliu.myblog_dev;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MapperScan("com.jiuliu.myblog_dev.mapper")  // 启用 Mapper 扫描
@SpringBootApplication
@EnableAspectJAutoProxy  // 启用 AspectJ 代理支持AOP
public class MyblogDevApplication {

    private static final Logger log = LoggerFactory.getLogger(MyblogDevApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(MyblogDevApplication.class, args);
    }

    @Bean
    public ApplicationRunner applicationRunner() {
        return args -> log.info("MyblogDev 应用就绪，可接受请求");
    }
}
