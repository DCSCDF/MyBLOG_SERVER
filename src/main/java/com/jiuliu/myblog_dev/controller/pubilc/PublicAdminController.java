/*
 * [PublicAdminController.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 */

package com.jiuliu.myblog_dev.controller.pubilc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.user.PublicAdminResponseDTO;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.utils.rateLimit.RateLimit;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共管理员信息接口 - 无需登录即可访问
 * 提供超级管理员用户的公开信息
 */
@RestController
@RequestMapping("/api/public/admin")
public class PublicAdminController {

    private final SysUserMapper sysUserMapper;

    public PublicAdminController(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 获取超级管理员用户的公开信息
     * GET /api/public/admin/info
     * 无需登录，所有用户均可访问
     * 返回字段：
     * - nickname: 昵称
     * - email: 邮箱
     * - avatarUrl: 头像URL
     * - bio: 用户简介
     */
    @RateLimit(count = 500, period = 1, prefix = "public_admin_info", ipBased = true)
    @GetMapping("/info")
    public Response<PublicAdminResponseDTO> getAdminInfo() {
        SysUser adminUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, "admin")
                .eq(SysUser::getIsDeleted, 0));

        if (adminUser == null) {
            return ResponseUtil.fail("管理员用户不存在", 404);
        }

        PublicAdminResponseDTO dto = new PublicAdminResponseDTO();
        dto.setNickname(adminUser.getNickname());
        // 脱敏邮箱，避免公开接口泄露管理员邮箱（定向钓鱼/撞库）
        dto.setEmail(maskEmail(adminUser.getEmail()));
        dto.setAvatarUrl(adminUser.getAvatarUrl());
        dto.setBio(adminUser.getBio());

        return ResponseUtil.success(dto, 200);
    }

    /**
     * 邮箱脱敏：保留首字符与 @ 后域名，中间打码（如 a***@example.com）
     */
    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "***" + domainPart;
        }
        return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + domainPart;
    }
}