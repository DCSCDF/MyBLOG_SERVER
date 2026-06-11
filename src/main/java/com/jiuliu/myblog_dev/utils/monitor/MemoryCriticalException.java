/*
 * [MemoryCriticalException.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/6/11
 */

package com.jiuliu.myblog_dev.utils.monitor;

/**
 * 内存严重不足异常，当可用内存低于阈值时抛出
 */
public class MemoryCriticalException extends RuntimeException {

    public MemoryCriticalException(String message) {
        super(message);
    }

}