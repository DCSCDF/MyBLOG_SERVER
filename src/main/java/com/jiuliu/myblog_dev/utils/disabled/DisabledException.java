/*
 * [DisabledException.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/27
 */

package com.jiuliu.myblog_dev.utils.disabled;

import lombok.Getter;

/**
 * 接口禁用异常
 * 当接口被 @Disabled 注解标记时抛出此异常
 */
@Getter
public class DisabledException extends RuntimeException {

    private final int code;

    public DisabledException() {
        super("该接口已禁用");
        this.code = 503;
    }

    public DisabledException(String message) {
        super(message);
        this.code = 503;
    }

    public DisabledException(String message, int code) {
        super(message);
        this.code = code;
    }

}
