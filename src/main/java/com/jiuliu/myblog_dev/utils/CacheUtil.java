/*
 * [CacheUtil.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/8
 */

package com.jiuliu.myblog_dev.utils;

/**
 * 内存缓存工具类
 * 提供缓存键常量定义，供各Service使用
 * 实际缓存实例在各Service中通过Guava CacheBuilder创建
 */
public class CacheUtil {

    /**
     * 系统配置缓存 - 缓存键前缀
     */
    public static final String CACHE_KEY_SYS_CONFIG = "sys_config:";
}
