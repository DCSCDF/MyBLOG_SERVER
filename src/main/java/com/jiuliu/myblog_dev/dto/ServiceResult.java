/*
 * [ServiceResult.java]
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

package com.jiuliu.myblog_dev.dto;

/**
 * 服务层统一返回结果。
 * code 默认 200，msg 默认 "ok"，使既有 Controller 的
 * handleServiceResult 逻辑与前端响应格式不变。
 */
public class ServiceResult {

    private int code;
    private String msg;
    private Object data;

    public ServiceResult() {
        this.code = 200;
        this.msg = "ok";
    }

    public ServiceResult(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static ServiceResult ok() {
        return new ServiceResult(200, "ok", null);
    }

    public static ServiceResult ok(String msg) {
        return new ServiceResult(200, msg, null);
    }

    public static ServiceResult data(Object data) {
        return new ServiceResult(200, "ok", data);
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
