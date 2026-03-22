/*
 * [PublicCategoryService.java]
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

package com.jiuliu.myblog_dev.service.blog;

import cn.dev33.satoken.util.SaResult;


/**
 * 公共分类 Service 接口
 * 提供公开的分类查询接口（不包含隐藏分类）
 */
public interface PublicCategoryService {

    /**
     * 获取可显示的分类列表
     * 返回所有未隐藏的分类，按排序顺序排列
     *
     * @return 分类列表
     */
    SaResult getVisibleCategories();

    /**
     * 根据ID获取分类详情
     * 仅返回未隐藏的分类
     *
     * @param categoryId 分类ID
     * @return 分类详情
     */
    SaResult getCategoryById(Long categoryId);

    /**
     * 清除公共分类缓存
     * 当后台对分类进行增删改操作时，需要调用此方法清除缓存
     */
    void clearPublicCategoryCache();
}
