/*
 * [RandomStringUtil.java]
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

package com.jiuliu.myblog_dev.utils.security;

import java.security.SecureRandom;

/**
 * 随机字符串工具。字符集：a-z A-Z 0-9。
 */
public final class RandomStringUtil {

    private static final char[] CHARS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private RandomStringUtil() {
    }

    public static String random(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be positive");
        }
        char[] result = new char[length];
        for (int i = 0; i < length; i++) {
            result[i] = CHARS[RANDOM.nextInt(CHARS.length)];
        }
        return new String(result);
    }
}
