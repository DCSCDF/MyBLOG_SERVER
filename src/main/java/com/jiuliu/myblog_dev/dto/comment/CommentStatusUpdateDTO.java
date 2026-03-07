/*
 * [CommentStatusUpdateDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/8
 */

package com.jiuliu.myblog_dev.dto.comment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新评论状态DTO（审核用）
 */
@Data
public class CommentStatusUpdateDTO {

    @NotNull(message = "状态不能为空")
    private Integer status;
}
