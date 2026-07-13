/*
 * [FriendLinkService.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/23
 */

package com.jiuliu.myblog_dev.service.link;

import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.link.FriendLinkCreateDTO;
import com.jiuliu.myblog_dev.dto.link.FriendLinkStatusUpdateDTO;
import com.jiuliu.myblog_dev.dto.link.FriendLinkUpdateDTO;
import com.jiuliu.myblog_dev.dto.link.PageFriendLinkDTO;

public interface FriendLinkService {

    SaResult getPageFriendLinks(PageFriendLinkDTO pageDto);

    SaResult createFriendLink(FriendLinkCreateDTO dto);

    SaResult updateFriendLink(FriendLinkUpdateDTO dto);

    /**
     * 变更友链审核状态：待审核→通过/拒绝，通过↔拒绝
     */
    SaResult updateFriendLinkStatus(Long id, FriendLinkStatusUpdateDTO dto);

    SaResult deleteFriendLink(Long id);

    /**
     * 清除友链缓存（包括后台管理和前台展示的缓存）
     */
    void clearFriendLinkCache();
}

