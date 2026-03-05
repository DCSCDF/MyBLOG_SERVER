/*
 * [BlogStatusUpdateDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/5
 */

package com.jiuliu.myblog_dev.dto.blog;

import lombok.Data;

@Data
public class BlogStatusUpdateDTO {

    /**
     * 是否隐藏 (null=不修改)
     */
    private Boolean isHidden;

//    /**
//     * 是否置顶 (null=不修改)
//     */
//    private Boolean isTop;
//
//    /**
//     * 是否推荐 (null=不修改)
//     */
//    private Boolean isRecommend;
}
