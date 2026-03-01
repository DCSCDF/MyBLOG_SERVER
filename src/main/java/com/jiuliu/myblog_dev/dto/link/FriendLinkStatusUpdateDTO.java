/*
 * [FriendLinkStatusUpdateDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/1
 */

package com.jiuliu.myblog_dev.dto.link;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 友链审核状态变更
 * 可将待审核改为通过/拒绝，或将通过改为拒绝、拒绝改为通过。
 * status: 0=待审核，1=已通过，2=已拒绝
 */
@Data
public class FriendLinkStatusUpdateDTO {

    @NotNull(message = "审核状态不能为空")
    @Min(value = 0, message = "状态只能为0待审核、1已通过、2已拒绝")
    @Max(value = 2, message = "状态只能为0待审核、1已通过、2已拒绝")
    private Integer status;
}
