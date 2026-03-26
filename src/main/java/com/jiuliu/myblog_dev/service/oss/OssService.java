/*
 * [OssService.java]
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

/**
 * OSS 对象存储服务接口
 */
public interface OssService {

    /**
     * 测试 OSS 连接
     *
     * @return SaResult
     */
    SaResult testConnection();
}
