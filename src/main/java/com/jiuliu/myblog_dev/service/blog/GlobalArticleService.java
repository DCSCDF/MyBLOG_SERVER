/*
 * [GlobalArticleService.java]
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

package com.jiuliu.myblog_dev.service.blog;

import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.blog.global.*;

/**
 * 全局文章管理Service接口
 */
public interface GlobalArticleService {

    /**
     * 分页获取所有文章列表
     *
     * @param dto 分页查询参数
     * @return 分页结果
     */
    SaResult getPageGlobalArticles(PageGlobalArticleDTO dto);

    /**
     * 更新文章状态（隐藏、置顶、推荐）
     *
     * @param blogId 文章ID
     * @param dto    状态更新参数
     * @return 操作结果
     */
    SaResult updateArticleStatus(Long blogId, GlobalArticleStatusUpdateDTO dto);

    /**
     * 删除文章
     *
     * @param blogId 文章ID
     * @return 操作结果
     */
    SaResult deleteArticle(Long blogId);

    /**
     * 清除全局文章列表缓存
     */
    void clearGlobalArticleCache();
}
