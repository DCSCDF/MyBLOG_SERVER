/*
 * [PublicFriendLinkService.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/27
 */

package com.jiuliu.myblog_dev.service.link;

import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.link.PagePublicFriendLinkDTO;
import com.jiuliu.myblog_dev.dto.link.PublicFriendLinkCreateDTO;

/**
 * 公共外链服务接口（无需登录）
 */
public interface PublicFriendLinkService {

    /**
     * 提交外链申请（提交后状态为待审核）
     *
     * @param dto 外链信息
     * @return 操作结果
     */
    SaResult createFriendLink(PublicFriendLinkCreateDTO dto);

    /**
     * 获取已通过审核的外链列表（分页）
     * 只返回 status=1 且未删除的外链
     * 按 sort_order 降序排序（数字越大越靠前）
     *
     * @param dto 分页参数
     * @return 分页结果
     */
    SaResult getPagePublicFriendLinks(PagePublicFriendLinkDTO dto);
}
