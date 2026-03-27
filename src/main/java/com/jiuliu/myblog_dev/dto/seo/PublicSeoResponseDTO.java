/*
 * [PublicSeoResponseDTO.java]
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

package com.jiuliu.myblog_dev.dto.seo;

import lombok.Data;

/**
 * 公共SEO响应DTO - 无需登录即可访问
 */
@Data
public class PublicSeoResponseDTO {

    private Long id;

    private String pageType;

    private Long pageId;

    private String title;

    private String keywords;

    private String description;

    private String ogTitle;

    private String ogDescription;

    private String ogImage;

    private String ogType;

    private String canonicalUrl;

    private String robots;
}
