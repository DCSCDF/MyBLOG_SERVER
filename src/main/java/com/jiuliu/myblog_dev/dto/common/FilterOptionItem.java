/*
 * [FilterOptionItem.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/18 11:52
 */

/*
 * [FilterOptionItem.java]
 * 列表筛选项：用于前端下拉等，value 为实际传参值，label 为展示名称。
 */
package com.jiuliu.myblog_dev.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilterOptionItem {

    /**
     * 筛选项值（如 0、1、"启用" 等，传参时使用）
     */
    private Object value;
    /**
     * 筛选项展示名称（如 "禁用"、"启用"、"是"、"否"）
     */
    private String label;
}
