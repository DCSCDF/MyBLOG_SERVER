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
 * UpdateTime: 2026/3/8 04:37
 */

package com.jiuliu.myblog_dev.utils.cache;

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

    /**
     * 用户评论列表缓存 - 缓存键前缀
     */
    public static final String CACHE_KEY_USER_COMMENT_LIST = "user_comment_list:";

    /**
     * 全局评论列表缓存 - 缓存键前缀
     */
    public static final String CACHE_KEY_GLOBAL_COMMENT_LIST = "global_comment_list:";

    /**
     * 文章列表缓存 - 缓存键前缀
     */
    public static final String CACHE_KEY_ARTICLE_LIST = "article_list:";

    /**
     * SEO列表缓存 - 缓存键前缀
     */
    public static final String CACHE_KEY_SEO_LIST = "seo_list:";

    /**
     * 友链列表缓存 - 缓存键前缀
     */
    public static final String CACHE_KEY_FRIEND_LINK_LIST = "friend_link_list:";

    /**
     * 分类列表缓存 - 缓存键前缀
     */
    public static final String CACHE_KEY_CATEGORY_LIST = "category_list:";

    /**
     * 公共文章列表缓存 - 缓存键前缀
     */
    public static final String CACHE_KEY_PUBLIC_ARTICLE_LIST = "public_article_list:";

    /**
     * OSS 图片缓存 - 缓存键前缀
     */
    public static final String CACHE_KEY_OSS_IMAGE = "oss_image:";
}
