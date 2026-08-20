/*
 * [PendingPasswordResetService.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/4/12
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
 * 找回密码待验证服务
 * 用于存储和管理找回密码流程中的临时信息
 */
@Service
public class PendingPasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PendingPasswordResetService.class);

    private static final String PENDING_RESET_PREFIX = "password_reset:";
    private static final String RATE_LIMIT_PREFIX = "password_reset_rate_limit:";

    private final SaTokenDao saTokenDao;

    public PendingPasswordResetService(SaTokenDao saTokenDao) {
        this.saTokenDao = saTokenDao;
    }

    /**
     * 待重置密码信息结构
     */
    @Setter
    @Getter
    public static class PendingPasswordReset {
        private Long userId;
        private String username;
        private String email;
        private String code;
        private LocalDateTime codeExpireTime;
        /**
         * 错误尝试次数（超过上限后验证码作废，防在线爆破）
         */
        private int attemptCount;

        public PendingPasswordReset() {
        }

        public PendingPasswordReset(Long userId, String username, String email, String code, LocalDateTime codeExpireTime) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.code = code;
            this.codeExpireTime = codeExpireTime;
        }
    }

    /**
     * 保存待重置密码信息
     *
     * @param userId        用户ID
     * @param username      用户名
     * @param email         邮箱
     * @param code          验证码
     * @param expireMinutes 过期时间（分钟）
     */
    public void savePendingPasswordReset(Long userId, String username, String email, String code, int expireMinutes) {
        String key = PENDING_RESET_PREFIX + email;
        PendingPasswordReset reset = new PendingPasswordReset(userId, username, email, code,
                LocalDateTime.now().plusMinutes(expireMinutes));
        saTokenDao.set(key, serialize(reset), expireMinutes * 60L);
        log.info("保存待重置密码信息，userId={}, username={}, email={}", userId, username, email);
    }

    /**
     * 根据邮箱获取待重置密码信息
     *
     * @param email 邮箱
     * @return 待重置密码信息，如果不存在或已过期返回null
     */
    public PendingPasswordReset getPendingPasswordReset(String email) {
        String key = PENDING_RESET_PREFIX + email;
        String data = saTokenDao.get(key);
        if (data == null) {
            return null;
        }
        return deserialize(data);
    }

    /**
     * 删除待重置密码信息
     *
     * @param email 邮箱
     */
    public void deletePendingPasswordReset(String email) {
        String key = PENDING_RESET_PREFIX + email;
        saTokenDao.delete(key);
        log.info("删除待重置密码信息，email={}", email);
    }

    /**
     * 检查频率限制
     *
     * @param identifier    标识符（可以是邮箱或用户名）
     * @param expireMinutes 频率限制时间（分钟）
     * @return 如果还在频率限制期内，返回剩余秒数；否则返回0
     */
    public long checkRateLimit(String identifier, int expireMinutes) {
        String key = RATE_LIMIT_PREFIX + identifier;
        String data = saTokenDao.get(key);
        if (data == null) {
            return 0;
        }
        try {
            LocalDateTime expireTime = LocalDateTime.parse(data);
            if (LocalDateTime.now().isAfter(expireTime)) {
                return 0;
            }
            return java.time.Duration.between(LocalDateTime.now(), expireTime).getSeconds();
        } catch (Exception e) {
            log.error("解析频率限制时间失败，identifier={}", identifier, e);
            return 0;
        }
    }

    /**
     * 设置频率限制
     *
     * @param identifier    标识符（可以是邮箱或用户名）
     * @param expireMinutes 频率限制时间（分钟）
     */
    public void setRateLimit(String identifier, int expireMinutes) {
        String key = RATE_LIMIT_PREFIX + identifier;
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(expireMinutes);
        saTokenDao.set(key, expireTime.toString(), expireMinutes * 60L);
        log.info("设置频率限制，identifier={}, expireTime={}", identifier, expireTime);
    }

    /**
     * 记录一次验证码错误尝试
     *
     * @param email 邮箱
     * @return 累计错误次数；记录不存在或已过期时返回 -1
     */
    public int recordFailedAttempt(String email) {
        String key = PENDING_RESET_PREFIX + email;
        PendingPasswordReset reset = getPendingPasswordReset(email);
        if (reset == null) {
            return -1;
        }
        reset.setAttemptCount(reset.getAttemptCount() + 1);
        long ttlSeconds = java.time.Duration.between(LocalDateTime.now(), reset.getCodeExpireTime()).getSeconds();
        if (ttlSeconds <= 0) {
            saTokenDao.delete(key);
            return -1;
        }
        saTokenDao.set(key, serialize(reset), ttlSeconds);
        return reset.getAttemptCount();
    }

    private String serialize(PendingPasswordReset reset) {
        return reset.getUserId() + "|" + reset.getUsername() + "|" + reset.getEmail() + "|" +
                reset.getCode() + "|" + reset.getCodeExpireTime().toString() + "|" + reset.getAttemptCount();
    }

    private PendingPasswordReset deserialize(String data) {
        try {
            String[] parts = data.split("\\|", -1);
            if (parts.length >= 5) {
                PendingPasswordReset reset = new PendingPasswordReset();
                reset.setUserId(Long.parseLong(parts[0]));
                reset.setUsername(parts[1]);
                reset.setEmail(parts[2]);
                reset.setCode(parts[3]);
                reset.setCodeExpireTime(LocalDateTime.parse(parts[4]));
                if (parts.length >= 6) {
                    reset.setAttemptCount(Integer.parseInt(parts[5]));
                }
                return reset;
            }
        } catch (Exception e) {
            log.error("反序列化待重置密码信息失败，data={}", data, e);
        }
        return null;
    }
}
