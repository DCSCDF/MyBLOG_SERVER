/*
 * [ChangeEmailPendingService.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/4/10
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
 * 邮箱变更待验证服务
 * 用于存储和管理更换邮箱时的验证码信息
 */
@Service
public class ChangeEmailPendingService {

    private static final Logger log = LoggerFactory.getLogger(ChangeEmailPendingService.class);

    private static final String PENDING_KEY_PREFIX = "change_email_pending:";

    private final SaTokenDao saTokenDao;

    public ChangeEmailPendingService(SaTokenDao saTokenDao) {
        this.saTokenDao = saTokenDao;
    }

    /**
     * 待变更邮箱信息结构
     */
    @Setter
    @Getter
    public static class PendingEmailChange {
        private Long userId;
        private String newEmail;
        private String code;
        private LocalDateTime codeExpireTime;
        /**
         * 错误尝试次数（超过上限后验证码作废，防在线爆破）
         */
        private int attemptCount;

        public PendingEmailChange() {
        }

        public PendingEmailChange(Long userId, String newEmail, String code, LocalDateTime codeExpireTime) {
            this.userId = userId;
            this.newEmail = newEmail;
            this.code = code;
            this.codeExpireTime = codeExpireTime;
        }
    }

    /**
     * 保存待变更邮箱信息
     *
     * @param userId        用户ID
     * @param newEmail      新邮箱
     * @param code          验证码
     * @param expireMinutes 过期时间（分钟）
     */
    public void savePendingEmailChange(Long userId, String newEmail, String code, int expireMinutes) {
        String key = PENDING_KEY_PREFIX + userId;
        PendingEmailChange pending = new PendingEmailChange(userId, newEmail, code,
                LocalDateTime.now().plusMinutes(expireMinutes));
        saTokenDao.set(key, serialize(pending), expireMinutes * 60L);
        log.info("保存待变更邮箱信息，userId={}, newEmail={}", userId, newEmail);
    }

    /**
     * 根据用户ID获取待变更邮箱信息
     *
     * @param userId 用户ID
     * @return 待变更邮箱信息，如果不存在或已过期返回null
     */
    public PendingEmailChange getPendingEmailChange(Long userId) {
        String key = PENDING_KEY_PREFIX + userId;
        String data = saTokenDao.get(key);
        if (data == null) {
            return null;
        }
        return deserialize(data);
    }

    /**
     * 删除待变更邮箱信息
     *
     * @param userId 用户ID
     */
    public void deletePendingEmailChange(Long userId) {
        String key = PENDING_KEY_PREFIX + userId;
        saTokenDao.delete(key);
        log.info("删除待变更邮箱信息，userId={}", userId);
    }

    /**
     * 记录一次验证码错误尝试
     *
     * @param userId 用户ID
     * @return 累计错误次数；记录不存在或已过期时返回 -1
     */
    public int recordFailedAttempt(Long userId) {
        String key = PENDING_KEY_PREFIX + userId;
        PendingEmailChange pending = getPendingEmailChange(userId);
        if (pending == null) {
            return -1;
        }
        pending.setAttemptCount(pending.getAttemptCount() + 1);
        long ttlSeconds = java.time.Duration.between(LocalDateTime.now(), pending.getCodeExpireTime()).getSeconds();
        if (ttlSeconds <= 0) {
            saTokenDao.delete(key);
            return -1;
        }
        saTokenDao.set(key, serialize(pending), ttlSeconds);
        return pending.getAttemptCount();
    }

    private String serialize(PendingEmailChange pending) {
        return pending.getUserId() + "|" + pending.getNewEmail() + "|" + pending.getCode() + "|" +
                pending.getCodeExpireTime().toString() + "|" + pending.getAttemptCount();
    }

    private PendingEmailChange deserialize(String data) {
        try {
            String[] parts = data.split("\\|", -1);
            if (parts.length >= 4) {
                PendingEmailChange pending = new PendingEmailChange();
                pending.setUserId(Long.parseLong(parts[0]));
                pending.setNewEmail(parts[1]);
                pending.setCode(parts[2]);
                pending.setCodeExpireTime(LocalDateTime.parse(parts[3]));
                if (parts.length >= 5) {
                    pending.setAttemptCount(Integer.parseInt(parts[4]));
                }
                return pending;
            }
        } catch (Exception e) {
            log.error("反序列化待变更邮箱信息失败，data={}", data, e);
        }
        return null;
    }
}