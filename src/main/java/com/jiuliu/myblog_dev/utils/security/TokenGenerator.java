package com.jiuliu.myblog_dev.utils.security;

import java.security.SecureRandom;

/** Random string generator for one-time codes and temporary tokens. */
public final class TokenGenerator {

    private static final char[] ALPHANUMERIC =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenGenerator() {
    }

    public static String randomString(int length) {
        char[] buf = new char[length];
        for (int i = 0; i < length; i++) {
            buf[i] = ALPHANUMERIC[RANDOM.nextInt(ALPHANUMERIC.length)];
        }
        return new String(buf);
    }
}
