/*
 * [ResponseUtil.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/8 04:37
 */

package com.jiuliu.myblog_dev.utils.response;

import com.jiuliu.myblog_dev.dto.Response;

/**
 * 响应工具类
 * 提供便捷的方法创建统一响应格式
 */
public class ResponseUtil {

    /**
     * 创建成功的响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应对象
     */
    public static <T> Response<T> success(T data) {
        return Response.newSuccess(data);
    }

    /**
     * 创建成功响应（带状态码）
     *
     * @param data 响应数据
     * @param code 状态码
     * @param <T>  数据类型
     * @return 成功响应对象
     */
    public static <T> Response<T> success(T data, Integer code) {
        return Response.newSuccess(data, code);
    }

    /**
     * 创建成功响应（无数据）
     *
     * @return 成功响应对象
     */
    public static Response<Void> success() {
        return Response.newSuccess(null);
    }

    /**
     * 创建成功响应（无数据，带状态码）
     *
     * @param code 状态码
     * @return 成功响应对象
     */
    public static Response<Void> success(Integer code) {
        return Response.newSuccess(null, code);
    }

    /**
     * 创建失败的响应
     *
     * @param errorMsg 错误信息
     * @param <T>      数据类型
     * @return 失败的响应对象
     */
    public static <T> Response<T> fail(String errorMsg) {
        return Response.newFail(errorMsg);
    }

    /**
     * 创建失败的响应（带状态码）
     *
     * @param errorMsg 错误信息
     * @param code     状态码
     * @param <T>      数据类型
     * @return 失败的响应对象
     */
    public static <T> Response<T> fail(String errorMsg, Integer code) {
        return Response.newFail(errorMsg, code);
    }

    /**
     * 创建失败的响应（带数据）
     *
     * @param errorMsg 错误信息
     * @param data     响应数据
     * @param <T>      数据类型
     * @return 失败的响应对象
     */
    public static <T> Response<T> fail(String errorMsg, T data) {
        return Response.newFail(errorMsg, data);
    }

    /**
     * 创建失败的响应（带数据和状态码）
     *
     * @param errorMsg 错误信息
     * @param data     响应数据
     * @param code     状态码
     * @param <T>      数据类型
     * @return 失败的响应对象
     */
    public static <T> Response<T> fail(String errorMsg, T data, Integer code) {
        return Response.newFail(errorMsg, data, code);
    }
}