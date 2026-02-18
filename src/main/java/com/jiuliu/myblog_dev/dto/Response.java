/*
 * [Response.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/16 20:13
 */

package com.jiuliu.myblog_dev.dto;

import lombok.Data;

/**
 * 统一响应包装类
 * 所有接口返回都使用此格式包装
 *
 * @param <T> 数据类型
 */
@Data
public class Response<T> {

    /**
     * 响应数据
     */
    private T data;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 创建成功的响应
     *
     * @param data 响应数据
     * @param <K>  数据类型
     * @return 成功响应对象
     */
    public static <K> Response<K> newSuccess(K data) {
        return newSuccess(data, 200);
    }

    /**
     * 创建成功响应（带状态码）
     *
     * @param data 响应数据
     * @param code 状态码
     * @param <K>  数据类型
     * @return 成功响应对象
     */
    public static <K> Response<K> newSuccess(K data, Integer code) {
        Response<K> response = new Response<>();
        response.setData(data);
        response.setSuccess(true);
        response.setCode(code);
        return response;
    }

    /**
     * 创建失败的响应
     *
     * @param errorMsg 错误信息
     * @param <K>      数据类型（Void 表示无数据）
     * @return 失败的响应对象
     */
    public static <K> Response<K> newFail(String errorMsg) {
        return newFail(errorMsg, 400);
    }

    /**
     * 创建失败的响应（带状态码）
     *
     * @param errorMsg 错误信息
     * @param code     状态码
     * @param <K>      数据类型
     * @return 失败的响应对象
     */
    public static <K> Response<K> newFail(String errorMsg, Integer code) {
        Response<K> response = new Response<>();
        response.setErrorMsg(errorMsg);
        response.setSuccess(false);
        response.setCode(code);
        return response;
    }

    /**
     * 创建失败的响应（带数据）
     *
     * @param errorMsg 错误信息
     * @param data     响应数据
     * @param <K>      数据类型
     * @return 失败的响应对象
     */
    public static <K> Response<K> newFail(String errorMsg, K data) {
        return newFail(errorMsg, data, 400);
    }

    /**
     * 创建失败的响应（带数据和状态码）
     *
     * @param errorMsg 错误信息
     * @param data     响应数据
     * @param code     状态码
     * @param <K>      数据类型
     * @return 失败的响应对象
     */
    public static <K> Response<K> newFail(String errorMsg, K data, Integer code) {
        Response<K> response = new Response<>();
        response.setErrorMsg(errorMsg);
        response.setData(data);
        response.setSuccess(false);
        response.setCode(code);
        return response;
    }
}