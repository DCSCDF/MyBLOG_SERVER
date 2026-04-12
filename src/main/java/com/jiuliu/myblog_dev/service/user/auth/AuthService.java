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
import com.jiuliu.myblog_dev.dto.user.auth.RegisterCodeRequestDTO;
import com.jiuliu.myblog_dev.dto.user.auth.ChangeEmailConfirmDTO;
import com.jiuliu.myblog_dev.dto.user.auth.ChangeEmailDTO;
import com.jiuliu.myblog_dev.dto.user.auth.RegisterConfirmDTO;
import com.jiuliu.myblog_dev.dto.user.auth.UpdateAvatarUrlDTO;
import com.jiuliu.myblog_dev.dto.user.auth.UpdateEmailDTO;
import com.jiuliu.myblog_dev.dto.user.auth.UpdateNicknameDTO;
import com.jiuliu.myblog_dev.dto.user.auth.FindPasswordCodeRequestDTO;
import com.jiuliu.myblog_dev.dto.user.auth.FindPasswordConfirmDTO;


public interface AuthService {

    SaResult getPublicKey();

    SaResult login(LoginDTO dto);

    /**
     * 使用 OAuth 授权码换取 token
     * @param code 授权码
     * @return 包含 token 的结果
     */
    SaResult exchangeCodeForToken(String code);

    SaResult getUserProfile(Long userId);

    SaResult logout();

    SaResult updatePassword(ChangePasswordDTO dto, Long currentUserId);

    SaResult register(RegisterDTO dto);

    /**
     * 请求发送注册验证码（当 reg.use-email 开启时使用）
     *
     * @param dto 注册验证码请求DTO
     * @return 结果
     */
    SaResult requestRegisterCode(RegisterCodeRequestDTO dto);

    /**
     * 确认注册（验证邮箱验证码并完成注册）
     *
     * @param dto 注册确认DTO
     * @return 结果
     */
    SaResult confirmRegister(RegisterConfirmDTO dto);

    SaResult updateNickname(UpdateNicknameDTO dto, Long currentUserId);

    SaResult updateAvatarUrl(UpdateAvatarUrlDTO dto, Long currentUserId);

    SaResult updateEmail(UpdateEmailDTO dto, Long currentUserId);

    /**
     * 请求发送邮箱变更验证码（当 reg.use-email 开启时使用）
     *
     * @param dto 邮箱变更验证码请求DTO
     * @param currentUserId 当前用户ID
     * @return 结果
     */
    SaResult requestChangeEmailCode(ChangeEmailDTO dto, Long currentUserId);

    /**
     * 确认邮箱变更（验证邮箱验证码并完成邮箱更换）
     *
     * @param dto 邮箱变更确认DTO
     * @param currentUserId 当前用户ID
     * @return 结果
     */
    SaResult confirmChangeEmail(ChangeEmailConfirmDTO dto, Long currentUserId);

    /**
     * 获取当前用户拥有的权限编码列表（包含父权限展开后的所有子权限）
     */
    SaResult getCurrentUserPermissions(Long currentUserId);

    /**
     * 请求发送找回密码验证码（当 reg.use-email 为 true 时使用）
     *
     * @param dto 找回密码验证码请求DTO
     * @return 结果
     */
    SaResult requestFindPasswordCode(FindPasswordCodeRequestDTO dto);

    /**
     * 确认找回密码（验证邮箱验证码并完成密码重置）
     *
     * @param dto 找回密码确认DTO
     * @return 结果
     */
    SaResult confirmFindPassword(FindPasswordConfirmDTO dto);
}