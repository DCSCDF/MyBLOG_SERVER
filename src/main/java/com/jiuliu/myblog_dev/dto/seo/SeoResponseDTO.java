/*
 * [SeoResponseDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/21
 */

package com.jiuliu.myblog_dev.dto.seo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SeoResponseDTO {

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
    private Boolean isSystem;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
