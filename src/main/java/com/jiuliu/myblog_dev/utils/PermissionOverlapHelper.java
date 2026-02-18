/*
 * [PermissionOverlapHelper.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/18 04:24
 */

/*
 * [PermissionOverlapHelper.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.utils;

/**
 * 权限重叠检查工具
 * 两个权限"重叠"指：相同、或存在父子关系（如 system:user 与 system:user:list）
 */
public final class PermissionOverlapHelper {

    private PermissionOverlapHelper() {
    }

    /**
     * 判断两个权限编码是否重叠（相同或存在父子关系）
     *
     * @param codeA 权限编码A
     * @param codeB 权限编码B
     * @return true 表示重叠，false 表示不重叠
     */
    public static boolean overlaps(String codeA, String codeB) {
        if (codeA == null || codeB == null) {
            return false;
        }
        if (codeA.equals(codeB)) {
            return true;
        }
        // A 是 B 的父级：B 以 "A:" 开头
        if (codeB.startsWith(codeA + ":")) {
            return true;
        }
        // B 是 A 的父级：A 以 "B:" 开头
        return codeA.startsWith(codeB + ":");
    }
}
