/*
 * [PublicCommentService.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/24
 */

package com.jiuliu.myblog_dev.service.comment;

import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.comment.PublicCommentCreateDTO;

/**
 * 公共评论服务接口（无需登录）
 */
public interface PublicCommentService {

    /**
     * 提交公共评论
     * 支持已登录用户和游客两种模式
     *
     * @param dto      评论提交参数
     * @param ipAddress 客户端IP地址
     * @param deviceInfo 客户端设备信息
     * @param isLogin   当前用户是否已登录
     * @param userId    已登录用户的ID（如果未登录则为null）
     * @param isAdmin   当前用户是否为管理员（仅当已登录时有效）
     * @return 提交结果
     */
    SaResult createComment(PublicCommentCreateDTO dto, String ipAddress, String deviceInfo,
                          boolean isLogin, Long userId, boolean isAdmin);
}
