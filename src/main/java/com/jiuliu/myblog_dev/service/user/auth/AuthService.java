/*
 * [AuthService.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/18 11:52
 */

package com.jiuliu.myblog_dev.service.user.auth;


import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.user.auth.ChangePasswordDTO;
import com.jiuliu.myblog_dev.dto.user.auth.LoginDTO;
import com.jiuliu.myblog_dev.dto.user.auth.RegisterDTO;
import com.jiuliu.myblog_dev.dto.user.auth.UpdateNicknameDTO;
import com.jiuliu.myblog_dev.dto.user.auth.UpdateAvatarUrlDTO;
import com.jiuliu.myblog_dev.dto.user.auth.UpdateEmailDTO;


public interface AuthService {

    SaResult getPublicKey();

    SaResult login(LoginDTO dto);

    SaResult getUserProfile(Long userId);

    SaResult logout();

    SaResult updatePassword(ChangePasswordDTO dto, Long currentUserId);

    SaResult register(RegisterDTO dto);

    SaResult updateNickname(UpdateNicknameDTO dto, Long currentUserId);

    SaResult updateAvatarUrl(UpdateAvatarUrlDTO dto, Long currentUserId);

    SaResult updateEmail(UpdateEmailDTO dto, Long currentUserId);

    /**
     * 获取当前用户拥有的权限编码列表（包含父权限展开后的所有子权限）
     */
    SaResult getCurrentUserPermissions(Long currentUserId);
}