package com.jiuliu.myblog_dev.utils.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenGeneratorTest {

    @Test
    void generatesRequestedLengthAlphanumeric() {
        String token = TokenGenerator.randomString(32);

        assertEquals(32, token.length());
        assertTrue(token.matches("[0-9a-zA-Z]+"));
    }

    @Test
    void generatesDistinctValues() {
        assertNotEquals(TokenGenerator.randomString(32), TokenGenerator.randomString(32));
    }
}
