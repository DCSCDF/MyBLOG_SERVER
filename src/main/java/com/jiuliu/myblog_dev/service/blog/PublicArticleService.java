/*
 * [PublicArticleService.java]
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

package com.jiuliu.myblog_dev.service.blog;

import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.blog.publicity.PagePublicArticleDTO;


/**
 * 公共文章Service接口
 */
public interface PublicArticleService {

    /**
     * 分页获取公共文章列表
     *
     * @param dto 分页查询参数
     * @return 分页结果
     */
    SaResult getPagePublicArticles(PagePublicArticleDTO dto);

    /**
     * 根据文章ID获取文章详情
     * 只能获取公开的文章，隐藏或删除的文章无法访问
     *
     * @param articleId 文章ID
     * @return 文章详情
     */
    SaResult getPublicArticleDetail(Long articleId);

    /**
     * 清除公共文章列表缓存
     * 当后台对文章进行增删改操作时，需要调用此方法清除缓存
     */
    void clearPublicArticleCache();
}
