package com.jiuliu.myblog_dev.common;

/**
 * Service-layer result wrapper returned by all services; code/msg/data
 * semantics are what controllers translate into the unified Response.
 */
public class ServiceResult {

    private int code;
    private String msg;
    private Object data;

    public ServiceResult() {
    }

    private ServiceResult(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static ServiceResult data(Object data) {
        return new ServiceResult(200, "ok", data);
    }

    public static ServiceResult ok() {
        return new ServiceResult(200, "ok", null);
    }

    public static ServiceResult ok(String msg) {
        return new ServiceResult(200, msg, null);
    }

    public static ServiceResult error(String msg) {
        return new ServiceResult(500, msg, null);
    }

    public int getCode() {
        return code;
    }

    public ServiceResult setCode(int code) {
        this.code = code;
        return this;
    }

    public String getMsg() {
        return msg;
    }

    public ServiceResult setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public Object getData() {
        return data;
    }

    public ServiceResult setData(Object data) {
        this.data = data;
        return this;
    }
}
