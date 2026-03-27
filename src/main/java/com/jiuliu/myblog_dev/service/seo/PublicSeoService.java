/*
 * [PublicSeoService.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/27
 */

package com.jiuliu.myblog_dev.service.seo;

import cn.dev33.satoken.util.SaResult;

/**
 * 公共SEO Service接口 - 无需登录即可访问
 */
public interface PublicSeoService {

    /**
     * 根据页面类型获取SEO配置
     * 如果传入pageId，则优先匹配 pageType + pageId 的组合
     * 否则匹配 pageType + pageId IS NULL 的配置
     *
     * @param pageType 页面类型
     * @param pageId   页面ID（可选，用于精确匹配）
     * @return SEO配置
     */
    SaResult getSeoByPageType(String pageType, Long pageId);
}
