/*
 * [OssImageChangedListener.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/26
 */

package com.jiuliu.myblog_dev.event;

import com.jiuliu.myblog_dev.service.oss.GlobalOssService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * OSS图片变更事件监听器
 * <p>监听图片上传或删除事件，清除全局OSS列表缓存</p>
 */
@Component
public class OssImageChangedListener {

    private static final Logger log = LoggerFactory.getLogger(OssImageChangedListener.class);

    private final GlobalOssService globalOssService;

    public OssImageChangedListener(GlobalOssService globalOssService) {
        this.globalOssService = globalOssService;
    }

    @EventListener
    public void onOssImageChanged(OssImageChangedEvent event) {
        log.debug("收到OSS图片变更事件，类型=[{}]，hash=[{}]", event.getEventType(), event.getHash());

        switch (event.getEventType()) {
            case UPLOAD -> {
                globalOssService.clearCache();
                log.info("已清除全局OSS列表缓存（图片上传）");
            }
            case DELETE -> {
                globalOssService.clearCache();
                log.info("已清除全局OSS列表缓存（图片删除）");
            }
            default -> log.warn("未知的OSS图片变更事件类型");
        }
    }
}
