/*
 * [GlobalOssService.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/26
 */

package com.jiuliu.myblog_dev.service.oss;

import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.oss.global.PageGlobalOssDTO;

/**
 * 全局OSS管理 Service 接口
 */
public interface GlobalOssService {

    /**
     * 分页获取所有OSS图片列表
     *
     * @param dto 分页查询参数
     * @return SaResult，包含分页结果
     */
    SaResult getPageGlobalOssImages(PageGlobalOssDTO dto);

    /**
     * 删除图片（管理员权限，可删除所有用户的图片）
     *
     * @param hash 图片哈希值
     * @return SaResult
     */
    SaResult deleteImage(String hash);

    /**
     * 清除全局OSS列表缓存
     */
    void clearCache();
}
