/*
 * [PublicFriendLinkController.java]
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

package com.jiuliu.myblog_dev.controller.pubilc;

import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.link.PagePublicFriendLinkDTO;
import com.jiuliu.myblog_dev.dto.link.PagePublicFriendLinkResponseDTO;
import com.jiuliu.myblog_dev.dto.link.PublicFriendLinkCreateDTO;
import com.jiuliu.myblog_dev.service.link.PublicFriendLinkService;
import com.jiuliu.myblog_dev.utils.rateLimit.RateLimit;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共外链接口 - 无需登录即可访问
 */
@RestController
@RequestMapping("/api/public/friend-link")
public class PublicFriendLinkController {

    private final PublicFriendLinkService publicFriendLinkService;

    public PublicFriendLinkController(PublicFriendLinkService publicFriendLinkService) {
        this.publicFriendLinkService = publicFriendLinkService;
    }

    /**
     * 通用方法：处理SaResult结果
     */
    private <T> Response<T> handleSaResult(SaResult saResult) {
        if (saResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            T data = (T) saResult.getData();
            return ResponseUtil.success(data, 200);
        }
        return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
    }

    /**
     * 提交外链申请
     * POST /api/public/friend-link
     * 无需登录，所有用户均可访问
     * 限流：每个IP每分钟最多提交4次
     */
    @RateLimit(count = 4, period = 1, prefix = "public_friend_link_create")
    @PostMapping
    public Response<Object> createFriendLink(@Valid @RequestBody PublicFriendLinkCreateDTO dto) {
        SaResult saResult = publicFriendLinkService.createFriendLink(dto);
        return handleSaResult(saResult);
    }

    /**
     * 获取已通过审核的外链列表（分页）
     * GET /api/public/friend-link/list
     * 无需登录，所有用户均可访问
     * 只返回 status=1 且未删除的外链
     * 按 sort_order 降序排序
     */
    @PostMapping("/list")
    public Response<PagePublicFriendLinkResponseDTO> getPagePublicFriendLinks(@Valid @RequestBody PagePublicFriendLinkDTO dto) {
        SaResult saResult = publicFriendLinkService.getPagePublicFriendLinks(dto);
        return handleSaResult(saResult);
    }
}
