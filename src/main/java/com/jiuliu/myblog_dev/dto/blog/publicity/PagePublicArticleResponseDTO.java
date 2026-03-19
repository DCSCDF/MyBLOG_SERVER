/*
 * [PagePublicArticleResponseDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/14
 */

package com.jiuliu.myblog_dev.dto.blog.publicity;

import lombok.Data;

import java.util.List;

/**
 * 公共文章列表分页响应DTO
 */
@Data
public class PagePublicArticleResponseDTO {

    private List<PublicArticleResponseDTO> records;

    private Long total;

    private Long size;

    private Long current;

    private Long pages;
}
