/*
 * [PageGlobalArticleResponseDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/6
 */

package com.jiuliu.myblog_dev.dto.blog.global;

import com.jiuliu.myblog_dev.dto.common.FilterOptionItem;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 全局文章管理分页响应DTO
 */
@Data
public class PageGlobalArticleResponseDTO {

    private List<GlobalArticleResponseDTO> records;

    private Long total;

    private Long size;

    private Long current;

    private Long pages;

    /**
     * 可用的筛选项，供前端渲染筛选控件
     */
    private Map<String, List<FilterOptionItem>> filterOptions;
}
