/*
 * [OssServiceImpl.java]
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

package com.jiuliu.myblog_dev.service.oss;

import cn.dev33.satoken.util.SaResult;
import com.aliyun.oss.OSS;
import com.jiuliu.myblog_dev.config.business.OSSConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OssServiceImpl implements OssService {

    private static final Logger log = LoggerFactory.getLogger(OssServiceImpl.class);

    private final OSSConfig ossConfig;

    public OssServiceImpl(OSSConfig ossConfig) {
        this.ossConfig = ossConfig;
    }

    @Override
    public SaResult testConnection() {
        log.debug("开始 OSS 连接测试...");

        if (!ossConfig.isConfigured()) {
            log.warn("OSS 连接测试失败：配置未完成");
            return SaResult.error("OSS 配置未完成，请先在系统配置中完成阿里云 OSS 相关配置").setCode(400);
        }

        log.debug("OSS 配置检查通过 - bucket=[{}], endpoint=[{}]",
                ossConfig.getBucket(), ossConfig.getEndpoint());

        OSS ossClient = ossConfig.getOssClient();
        if (ossClient == null) {
            log.error("OSS 连接测试失败：无法获取 OSS 客户端");
            return SaResult.error("OSS 客户端初始化失败，请检查配置").setCode(500);
        }

        try {
            String bucket = ossConfig.getBucket();
            log.debug("尝试列出 Bucket 中的对象，bucket=[{}]", bucket);
            ossClient.listObjects(bucket);
            log.info("OSS 连接测试成功，bucket={}", bucket);
            return SaResult.data("OSS 配置已完成且连接正常");
        } catch (Exception e) {
            log.warn("OSS 连接测试失败：{}", e.getMessage());
            log.warn("OSS 连接错误类型：{}", e.getClass().getName());
            return SaResult.error("OSS 配置已完成，但无法连接到服务器：" + e.getMessage()).setCode(400);
        }
    }
}
