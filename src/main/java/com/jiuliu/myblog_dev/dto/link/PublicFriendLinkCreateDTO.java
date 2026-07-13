/*
 * [PublicFriendLinkCreateDTO.java]
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

package com.jiuliu.myblog_dev.dto.link;

import com.jiuliu.myblog_dev.utils.validation.ValidUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公共外链提交DTO
 */
@Data
public class PublicFriendLinkCreateDTO {

    /**
     * 链接名称
     */
    @NotBlank(message = "链接名称不能为空")
    @Size(max = 100, message = "链接名称不能超过100个字符")
    private String name;

    /**
     * URL 地址
     */
    @NotBlank(message = "URL地址不能为空")
    @Size(max = 500, message = "URL地址不能超过500个字符")
    @ValidUrl(message = "URL地址格式无效，请输入有效的网址")
    private String url;

    /**
     * 简介
     */
    @Size(max = 500, message = "简介不能超过500个字符")
    private String summary;

    /**
     * 站点图片URL
     */
    @Size(max = 500, message = "站点图片URL不能超过500个字符")
    @ValidUrl(message = "站点图片URL格式无效，请输入有效的网址")
    private String imageUrl;
}
