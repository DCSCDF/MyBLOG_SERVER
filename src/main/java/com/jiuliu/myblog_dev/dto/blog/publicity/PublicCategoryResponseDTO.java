/*
 * [PublicCategoryResponseDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/23
 */

package com.jiuliu.myblog_dev.dto.blog.publicity;

import lombok.Data;

/**
 * 公共分类列表响应DTO
 * 用于公开接口，返回可显示的分类信息（不包含隐藏分类）
 */
@Data
public class PublicCategoryResponseDTO {

    /**
     * 分类ID
     */
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 排序顺序，数字越大越靠前
     */
    private Integer sortOrder;
}
