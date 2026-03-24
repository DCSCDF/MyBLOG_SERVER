/*
 * [PublicCommentController.java]
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

package com.jiuliu.myblog_dev.controller.pubilc;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.comment.PublicCommentCreateDTO;
import com.jiuliu.myblog_dev.service.comment.PublicCommentService;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共评论接口 - 无需登录即可访问
 */
@RestController
@RequestMapping("/api/public/comment")
public class PublicCommentController {

    private static final Logger log = LoggerFactory.getLogger(PublicCommentController.class);

    private final PublicCommentService publicCommentService;

    public PublicCommentController(PublicCommentService publicCommentService) {
        this.publicCommentService = publicCommentService;
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
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            ip = ip.split(",")[0].trim();
            return ip;
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    /**
     * 提交公共评论
     * POST /api/public/comment
     * 无需登录，所有用户均可访问
     * 已登录用户使用用户表信息，游客使用传入的信息
     */
    @PostMapping
    public Response<Object> createComment(@Valid @RequestBody PublicCommentCreateDTO dto,
                                          HttpServletRequest request) {
        // 获取客户端信息
        String ipAddress = getClientIpAddress(request);
        String deviceInfo = request.getHeader("User-Agent");

        // 检查用户登录状态
        boolean isLogin = StpUtil.isLogin();
        Long userId = null;
        boolean isAdmin = false;

        if (isLogin) {
            try {
                userId = StpUtil.getLoginIdAsLong();
                // 检查是否为管理员（拥有任意系统管理权限即视为管理员）
                isAdmin = StpUtil.hasPermission("admin:login")
                        || StpUtil.hasPermission("system:user:list")
                        || StpUtil.hasPermission("system:blog:list");
            } catch (Exception e) {
                log.warn("获取登录用户信息失败", e);
                isLogin = false;
            }
        }

        SaResult saResult = publicCommentService.createComment(dto, ipAddress, deviceInfo, isLogin, userId, isAdmin);
        return handleSaResult(saResult);
    }
}
