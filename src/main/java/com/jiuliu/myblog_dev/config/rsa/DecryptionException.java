/*
 * [DecryptionException.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/11 00:10
 */

package com.jiuliu.myblog_dev.config.rsa;

public class DecryptionException extends RuntimeException {
    public DecryptionException(String message) {
        super(message);
    }
}