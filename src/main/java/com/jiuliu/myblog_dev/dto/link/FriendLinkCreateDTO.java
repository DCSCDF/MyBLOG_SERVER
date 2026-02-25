/*
 * [FriendLinkCreateDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/23
 */

package com.jiuliu.myblog_dev.dto.link;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建外链/友情链接
 */
@Data
public class FriendLinkCreateDTO {

    @NotBlank(message = "链接名称不能为空")
    private String name;

    @NotBlank(message = "URL地址不能为空")
    private String url;

    /**
     * 简介
     */
    private String summary;

    /**
     * 备注
     */
    private String remark;

    /**
     * 图片 URL
     */
    private String imageUrl;

    /**
     * 排序顺序（数字越大越靠前）
     */
    private Integer sortOrder;
}

