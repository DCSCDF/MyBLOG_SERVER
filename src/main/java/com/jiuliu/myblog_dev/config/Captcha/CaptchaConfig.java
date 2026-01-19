package com.jiuliu.myblog_dev.config.Captcha;

import com.anji.captcha.model.common.Const;
import com.anji.captcha.service.CaptchaService;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class CaptchaConfig {

    @Bean
    public CaptchaService captchaService() {
        Properties config = new Properties();
        // 1. 设置缓存类型 (核心配置)
        // 对于单机环境，使用 'local'（内存缓存）
        // 对于分布式环境，使用 'redis'，但您必须提供 CaptchaCacheService 的 Redis 实现
        config.put(Const.CAPTCHA_CACHETYPE, "local");

        // 2. 设置验证码类型
        config.put(Const.CAPTCHA_TYPE, "blockPuzzle"); // 滑块拼图验证码

        // 3. 其他可选配置 (可参考官方文档按需添加)
        config.put(Const.CAPTCHA_WATER_MARK, ""); // 水印文字
        config.put(Const.CAPTCHA_SLIP_OFFSET, "5"); // 滑动允许的误差偏移量
        // config.put(Const.ORIGINAL_PATH_JIGSAW, "classpath:images/jigsaw"); // 底图路径（如果使用自定义图片）

        // 通过工厂类创建 CaptchaService 实例
        return CaptchaServiceFactory.getInstance(config);
    }
}