/*
 * [SeoCreateDTO.java]
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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SeoCreateDTO {

    /** 页面类型名称，可从筛选项选择或自定义输入，同一类型名称在同一 pageId 下不可重复 */
    @NotBlank(message = "页面类型不能为空")
    @Size(max = 50, message = "页面类型最大50字符")
    private String pageType;

    private Long pageId;              // 关联页面ID（文章ID、分类ID等，首页等固定页面可为空）

    @Size(max = 200, message = "SEO标题最大200字符")
    private String title;

    @Size(max = 500, message = "SEO关键词最大500字符")
    private String keywords;

    @Size(max = 1500, message = "SEO描述最大1500字符")
    private String description;

    @Size(max = 200, message = "Open Graph标题最大200字符")
    private String ogTitle;

    @Size(max = 1500, message = "Open Graph描述最大1500字符")
    private String ogDescription;

    @Size(max = 500, message = "Open Graph图片URL最大500字符")
    private String ogImage;

    @Size(max = 50, message = "Open Graph类型最大50字符")
    private String ogType;

    @Size(max = 500, message = "规范URL最大500字符")
    private String canonicalUrl;

    @Size(max = 100, message = "robots标签最大100字符")
    private String robots;
}
