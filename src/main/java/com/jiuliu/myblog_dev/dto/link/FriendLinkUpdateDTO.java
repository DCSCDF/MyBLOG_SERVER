/*
 * [FriendLinkUpdateDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/23
 */

package com.jiuliu.myblog_dev.dto.link;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新外链/友情链接
 */
@Data
public class FriendLinkUpdateDTO {

    @NotNull(message = "ID不能为空")
    private Long id;

    private String name;

    private String url;

    private String summary;

    private String remark;

    private String imageUrl;

    private Integer sortOrder;
}

