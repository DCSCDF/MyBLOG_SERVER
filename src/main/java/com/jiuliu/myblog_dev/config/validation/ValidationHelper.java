/*
 * [ValidationHelper.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/17 22:40
 */

package com.jiuliu.myblog_dev.config.validation;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * 验证帮助类
 */
public class ValidationHelper {

    private static final Logger log = LoggerFactory.getLogger(ValidationHelper.class);

    // 不允许的特殊字符
    private static final String INVALID_CHARS = " \t\n\r\"'`;=<>\\/";

    /**
     * 验证用户名
     *
     */
    public static boolean validateUsername(String username) {
        if (!StringUtils.hasText(username)) {
            log.warn("用户名不能为空");
            return true;
        }

        // 检查长度
        if (username.length() < 4 || username.length() > 20) {
            log.warn("用户名长度必须在4-20之间");
            return true;

        }

        // 检查特殊字符
        if (containsInvalidChars(username)) {
            log.warn("用户名不能包含空格、引号、分号等特殊字符");
            return true;

        }

        // 检查格式
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            log.warn("用户名只能包含字母、数字和下划线");
            return true;

        }
        return false;
    }

    /**
     * 验证密码格式是否符合要求
     *
     * @param password 待验证的密码
     * @return true 表示密码有效，false 表示密码无效
     */
    public static boolean validatePassword(String password) {
        if (!StringUtils.hasText(password)) {
            log.warn("密码不能为空");
            return true;
        }

        // 检查长度
        if (password.length() < 6 || password.length() > 20) {
            log.warn("密码长度必须在6-20之间");
            return true;
        }

        // 检查特殊字符
        if (containsInvalidChars(password)) {
            log.warn("密码不能包含空格、引号、分号等特殊字符");
            return true;
        }

        // 检查是否同时包含字母和数字
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");

        if (!hasLetter) {
            log.warn("密码必须包含字母");
            return true;
        }

        if (!hasDigit) {
            log.warn("密码必须包含数字");
            return true;
        }
        return false;
    }

    /**
     * 验证邮箱
     */
    public static boolean validateEmail(String email) {
        if (!StringUtils.hasText(email)) {
            log.warn("邮箱不能为空");
            return false;
        }

        // 简单的邮箱格式验证
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            log.warn("邮箱格式不正确");
            return false;
        }
        return true;
    }


    /**
     * 检查是否包含非法字符
     */
    public static boolean containsInvalidChars(String str) {
        for (char c : str.toCharArray()) {
            if (INVALID_CHARS.indexOf(c) != -1) {
                return true;
            }
        }
        return false;
    }


}