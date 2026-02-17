/*
 * [BusinessException.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.exception;

import lombok.Getter;

/**
 * 业务异常类
 * 用于统一处理业务逻辑中的异常情况
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message, int code) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, int code, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

}