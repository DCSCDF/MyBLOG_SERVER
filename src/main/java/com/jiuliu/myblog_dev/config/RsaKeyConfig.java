/*
 * [RsaKeyConfig.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/1/18 11:53
 */

package com.jiuliu.myblog_dev.config;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Configuration
@Getter // Lombok 自动生成 getter 方法
public class RsaKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyConfig.class);
    private static final int KEY_SIZE = 2048;
    private static final long REFRESH_INTERVAL_HOURS = 720;

    private String publicKeyBase64;
    private String privateKeyBase64;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        generateKeyPair();
        // 每 REFRESH_INTERVAL_HOURS 小时刷新一次
        scheduler.scheduleAtFixedRate(this::generateKeyPair,
                REFRESH_INTERVAL_HOURS, REFRESH_INTERVAL_HOURS, TimeUnit.HOURS);
    }

    private synchronized void generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(KEY_SIZE);
            KeyPair keyPair = keyGen.generateKeyPair();

            this.publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            this.privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());


            log.info("RSA 密钥已刷新 (有效期: {} 小时)", REFRESH_INTERVAL_HOURS);
        } catch (NoSuchAlgorithmException e) {
            // 记录异常但不泄露密钥
            log.error("RSA 密钥生成失败: {}", e.getMessage(), e);
            throw new RuntimeException("无法生成 RSA 密钥对", e);
        }
    }
}