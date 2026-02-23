/*
 * [SysSeo.java]
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

package com.jiuliu.myblog_dev.entity.seo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_seo")
public class SysSeo {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("page_type")
    private String pageType;          // 页面类型名称（来自系统默认或用户自定义，筛选项为库中不重复值）

    @TableField("page_id")
    private Long pageId;              // 关联页面ID（文章ID、分类ID等，首页等固定页面可为空）

    private String title;              // SEO标题（title标签）

    private String keywords;           // SEO关键词（keywords meta标签，逗号分隔）

    private String description;        // SEO描述（description meta标签）

    @TableField("og_title")
    private String ogTitle;           // Open Graph标题（og:title）

    @TableField("og_description")
    private String ogDescription;      // Open Graph描述（og:description）

    @TableField("og_image")
    private String ogImage;           // Open Graph图片URL（og:image）

    @TableField("og_type")
    private String ogType;            // Open Graph类型（og:type，如website、article）

    @TableField("canonical_url")
    private String canonicalUrl;      // 规范URL（canonical link）

    private String robots;             // robots meta标签（如index,follow、no index,no follow）

    @TableField("is_deleted")
    private Integer isDeleted;         // 0=未删除，1=已删除

    @TableField("is_system")
    private Boolean isSystem;          // 是否系统内置：0=否，1=是（不可删除）

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
