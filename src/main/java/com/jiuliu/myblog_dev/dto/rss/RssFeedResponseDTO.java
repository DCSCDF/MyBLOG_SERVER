/*
 * [RssFeedResponseDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/29
 */

package com.jiuliu.myblog_dev.dto.rss;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RSS Feed 响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RssFeedResponseDTO {

    /** RSS Feed 的原始 XML 字符串 */
    private String feedXml;

    /** 文章总数 */
    private Integer articleCount;

    /** 文章标题列表（用于日志或调试） */
    private List<String> articleTitles;
}
