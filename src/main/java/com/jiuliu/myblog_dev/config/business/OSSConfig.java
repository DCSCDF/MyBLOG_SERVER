/*
 * [OSSConfig.java]
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

package com.jiuliu.myblog_dev.config.business;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.jiuliu.myblog_dev.mapper.config.SysConfigMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 对象存储配置类
 *
 * <p>该类负责初始化和管理阿里云 OSS 客户端，提供以下功能：</p>
 * <ul>
 *   <li>从数据库动态加载 OSS 配置信息</li>
 *   <li>初始化和管理 OSS 客户端实例</li>
 *   <li>配置刷新（无需重启即可更新配置）</li>
 * </ul>
 *
 * <p><b>配置项说明：</b></p>
 * <table border="1">
 *   <tr><th>配置键</th><th>说明</th></tr>
 *   <tr><td>aliyun.Access-key</td><td>阿里云 AccessKey ID</td></tr>
 *   <tr><td>aliyun.Secret-key</td><td>阿里云 AccessKey Secret</td></tr>
 *   <tr><td>aliyun.Bucket</td><td>OSS Bucket 名称</td></tr>
 *   <tr><td>aliyun.end-point</td><td>OSS 访问域名（如 oss-cn-hangzhou.aliyuncs.com）</td></tr>
 *   <tr><td>aliyun.https-enabled</td><td>是否启用 HTTPS（true/false）</td></tr>
 * </table>
 *
 * @author Jiu Liu
 * @see <a href="https://help.aliyun.com/document_detail/32017.html">阿里云 OSS Java SDK 文档</a>
 */
@Component
public class OSSConfig {

    private static final Logger log = LoggerFactory.getLogger(OSSConfig.class);

    /**
     * 阿里云 AccessKey ID 配置键
     */
    private static final String KEY_ACCESS_KEY = "aliyun.Access-key";
    /**
     * 阿里云 AccessKey Secret 配置键
     */
    private static final String KEY_SECRET_KEY = "aliyun.Secret-key";
    /**
     * OSS Bucket 名称配置键
     */
    private static final String KEY_BUCKET = "aliyun.Bucket";
    /**
     * OSS 访问域名配置键
     */
    private static final String KEY_ENDPOINT = "aliyun.end-point";
    /**
     * HTTPS 启用配置键
     */
    private static final String KEY_HTTPS_ENABLED = "aliyun.https-enabled";

    /**
     * 系统配置 Mapper
     */
    private final SysConfigMapper sysConfigMapper;

    /**
     * OSS 客户端实例
     */
    private OSS ossClient;
    /**
     * 访问密钥 ID
     */
    private String accessKey;
    /**
     * 访问密钥密钥
     */
    private String secretKey;
    /**
     * Bucket 名称
     */
    @Getter
    private String bucket;
    /**
     * OSS 访问域名
     */
    @Getter
    private String endpoint;
    /**
     * 是否启用 HTTPS
     */
    @Getter
    private boolean httpsEnabled;

    /**
     * 构造函数
     *
     * @param sysConfigMapper 系统配置 Mapper，用于从数据库读取 OSS 配置
     */
    public OSSConfig(SysConfigMapper sysConfigMapper) {
        this.sysConfigMapper = sysConfigMapper;
    }

    /**
     * 初始化方法
     *
     * <p>在 Spring 容器创建该 Bean 后自动调用，加载 OSS 配置并初始化客户端。</p>
     */
    @PostConstruct
    public void init() {
        refreshConfiguration();
    }

    /**
     * 刷新 OSS 配置
     *
     * <p>从数据库重新加载所有 OSS 配置项，并重新初始化 OSS 客户端。
     * 该方法线程安全，可并发调用。</p>
     */
    public synchronized void refreshConfiguration() {
        this.accessKey = getConfigValue(KEY_ACCESS_KEY);
        this.secretKey = getConfigValue(KEY_SECRET_KEY);
        this.bucket = getConfigValue(KEY_BUCKET);
        this.endpoint = getConfigValue(KEY_ENDPOINT);
        String https = getConfigValue(KEY_HTTPS_ENABLED);
        this.httpsEnabled = "true".equalsIgnoreCase(https);

        log.info("OSS 配置刷新：bucket=[{}], endpoint=[{}], https=[{}]",
                bucket, endpoint, httpsEnabled);
        log.debug("OSS 原始配置 - AccessKey=[{}], SecretKey=[{}], rawHttps=[{}]",
                maskSecret(accessKey), maskSecret(secretKey), https);

        initOssClient();
    }

    /**
     * 初始化 OSS 客户端
     *
     * <p>使用当前配置创建 OSS 客户端实例。
     * 如果配置不完整，客户端将设为 null。</p>
     */
    private void initOssClient() {
        try {
            if (accessKey == null || accessKey.isBlank() || "Access".equals(accessKey) ||
                    secretKey == null || secretKey.isBlank() || "Secret".equals(secretKey) ||
                    endpoint == null || endpoint.isBlank() || "end-point".equals(endpoint) ||
                    bucket == null || bucket.isBlank() || "Bucket".equals(bucket)) {
                log.warn("OSS 配置未完成，请先在系统配置中完成阿里云 OSS 相关配置");
                this.ossClient = null;
                return;
            }

            // 构建完整 endpoint URL
            String protocol = httpsEnabled ? "https://" : "http://";
            String fullEndpoint = protocol + endpoint;
            log.debug("OSS 客户端初始化 - 完整 endpoint=[{}], bucket=[{}]", fullEndpoint, bucket);
            log.debug("OSS 请求将访问的域名=[{}.{}]", bucket, endpoint);

            // 使用旧版 API 直接创建客户端
            this.ossClient = new OSSClientBuilder().build(fullEndpoint, accessKey, secretKey);

            log.info("OSS 客户端初始化成功，endpoint={}, bucket={}", fullEndpoint, bucket);
        } catch (Exception e) {
            log.error("OSS 客户端初始化失败: {}", e.getMessage(), e);
            this.ossClient = null;
        }
    }

    /**
     * 脱敏密钥信息（只显示首尾各2位）
     */
    private String maskSecret(String value) {
        if (value == null || value.length() <= 4) {
            return "***";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }

    /**
     * 检查 OSS 是否已配置
     *
     * @return 如果所有配置项都已填写则返回 true，否则返回 false
     */
    public boolean isConfigured() {
        return accessKey != null && !accessKey.isBlank() && !"Access".equals(accessKey) &&
                secretKey != null && !secretKey.isBlank() && !"Secret".equals(secretKey) &&
                endpoint != null && !endpoint.isBlank() && !"end-point".equals(endpoint) &&
                bucket != null && !bucket.isBlank() && !"Bucket".equals(bucket);
    }

    /**
     * 获取 OSS 客户端实例
     *
     * <p>每次调用都从数据库读取最新配置并重新初始化客户端。</p>
     *
     * @return OSS 客户端实例，如果未配置则返回 null
     */
    public OSS getOssClient() {
        refreshConfiguration();
        return ossClient;
    }

    /**
     * 从数据库获取配置值
     *
     * @param key 配置键
     * @return 配置值，如果获取失败则返回 null
     */
    private String getConfigValue(String key) {
        try {
            return sysConfigMapper.selectValueByKey(key);
        } catch (Exception e) {
            log.warn("获取配置失败: {}: {}", key, e.getMessage());
        }
        return null;
    }
}
