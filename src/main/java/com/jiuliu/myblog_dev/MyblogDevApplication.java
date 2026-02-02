/*
 * [MyblogDevApplication.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/1/21 01:11
 */

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
