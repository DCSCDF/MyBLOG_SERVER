/*
 * [CaptchaConfig.java]
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

package com.jiuliu.myblog_dev.config.business;

import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.resource.ResourceStore;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import cloud.tianai.captcha.resource.impl.LocalMemoryResourceStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 行为验证码资源配置，使用项目自身的 CORS 配置，并从 resources 加载背景图片。
 */
@Configuration
public class CaptchaConfig {

    /**
     * 使用 classpath 下的本地 PNG 图片。
     * 具体图片文件需放在 resources/images 目录中，例如 a.png、b.png、c.png、48.png。
     */
    @Bean
    public ResourceStore resourceStore() {
        LocalMemoryResourceStore resourceStore = new LocalMemoryResourceStore();


        // 滑块验证码背景图（resources/images，PNG）
        resourceStore.addResource(CaptchaTypeConstant.SLIDER, new Resource("classpath", "images/a.png", "default"));
        resourceStore.addResource(CaptchaTypeConstant.SLIDER, new Resource("classpath", "images/b.png", "default"));
        resourceStore.addResource(CaptchaTypeConstant.SLIDER, new Resource("classpath", "images/c.png", "default"));


        // 旋转验证码、滑动还原、文字点选验证码背景图（resources/images，PNG）
        resourceStore.addResource(CaptchaTypeConstant.ROTATE, new Resource("classpath", "images/a.png", "default"));
        resourceStore.addResource(CaptchaTypeConstant.CONCAT, new Resource("classpath", "images/b.png", "default"));
        resourceStore.addResource(CaptchaTypeConstant.WORD_IMAGE_CLICK, new Resource("classpath", "images/c.png", "default"));

        return resourceStore;
    }
}