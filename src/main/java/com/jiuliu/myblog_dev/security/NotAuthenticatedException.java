package com.jiuliu.myblog_dev.security;

/**
 * Thrown when code requires a logged-in user but the SecurityContext is
 * anonymous. Handled as HTTP 401 by GlobalExceptionHandler.
 */
public class NotAuthenticatedException extends RuntimeException {

    public NotAuthenticatedException(String message) {
        super(message);
    }
}
