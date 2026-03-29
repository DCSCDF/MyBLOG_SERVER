/*
 * [RssFeedService.java]
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

package com.jiuliu.myblog_dev.service.rss;

import com.jiuliu.myblog_dev.dto.rss.RssFeedResponseDTO;

/**
 * RSS Feed Service接口
 */
public interface RssFeedService {

    /**
     * 生成最新文章的RSS Feed
     * 按时间倒序排列，返回最新的10篇文章
     *
     * @return RSS Feed XML 和文章信息
     */
    RssFeedResponseDTO generateRssFeed();
}
