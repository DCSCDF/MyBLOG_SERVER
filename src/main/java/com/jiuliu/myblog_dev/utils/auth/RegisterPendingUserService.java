/*
 * [RegisterPendingUserService.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/4/7
 */

package com.jiuliu.myblog_dev.utils.auth;

import cn.dev33.satoken.dao.SaTokenDao;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 注册待验证用户服务
 * 用于存储和管理邮箱验证码注册流程中的临时用户信息
 */
@Service
public class RegisterPendingUserService {

    private static final Logger log = LoggerFactory.getLogger(RegisterPendingUserService.class);

    private static final String PENDING_USER_PREFIX = "register_pending:";

    private final SaTokenDao saTokenDao;

    public RegisterPendingUserService(SaTokenDao saTokenDao) {
        this.saTokenDao = saTokenDao;
    }

    /**
     * 待注册用户信息结构
     */
    @Setter
    @Getter
    public static class PendingUser {
        private String username;
        private String email;
        private String password;
        private String code;
        private LocalDateTime codeExpireTime;

        public PendingUser() {
        }

        public PendingUser(String username, String email, String password, String code, LocalDateTime codeExpireTime) {
            this.username = username;
            this.email = email;
            this.password = password;
            this.code = code;
            this.codeExpireTime = codeExpireTime;
        }

    }

    /**
     * 保存待注册用户信息
     *
     * @param email           邮箱
     * @param username        用户名
     * @param encodedPassword 加密后的密码
     * @param code            验证码
     * @param expireMinutes   过期时间（分钟）
     */
    public void savePendingUser(String email, String username, String encodedPassword, String code, int expireMinutes) {
        String key = PENDING_USER_PREFIX + email;
        PendingUser user = new PendingUser(username, email, encodedPassword, code,
                LocalDateTime.now().plusMinutes(expireMinutes));
        saTokenDao.set(key, serialize(user), expireMinutes * 60L);
        log.info("保存待注册用户信息，email={}, username={}", email, username);
    }

    /**
     * 根据邮箱获取待注册用户信息
     *
     * @param email 邮箱
     * @return 待注册用户信息，如果不存在或已过期返回null
     */
    public PendingUser getPendingUser(String email) {
        String key = PENDING_USER_PREFIX + email;
        String data = saTokenDao.get(key);
        if (data == null) {
            return null;
        }
        return deserialize(data);
    }

    /**
     * 删除待注册用户信息
     *
     * @param email 邮箱
     */
    public void deletePendingUser(String email) {
        String key = PENDING_USER_PREFIX + email;
        saTokenDao.delete(key);
        log.info("删除待注册用户信息，email={}", email);
    }

    private String serialize(PendingUser user) {
        return user.getUsername() + "|" + user.getEmail() + "|" + user.getPassword() + "|" +
                user.getCode() + "|" + user.getCodeExpireTime().toString();
    }

    private PendingUser deserialize(String data) {
        try {
            String[] parts = data.split("\\|", -1);
            if (parts.length >= 5) {
                PendingUser user = new PendingUser();
                user.setUsername(parts[0]);
                user.setEmail(parts[1]);
                user.setPassword(parts[2]);
                user.setCode(parts[3]);
                user.setCodeExpireTime(LocalDateTime.parse(parts[4]));
                return user;
            }
        } catch (Exception e) {
            log.error("反序列化待注册用户信息失败，data={}", data, e);
        }
        return null;
    }
}
