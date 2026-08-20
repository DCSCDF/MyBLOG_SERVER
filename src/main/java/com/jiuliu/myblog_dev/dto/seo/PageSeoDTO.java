/*
 * [PageSeoDTO.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/21
 */

package com.jiuliu.myblog_dev.dto.seo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PageSeoDTO {

    @NotNull(message = "当前页码不能为空")
    @Min(value = 1, message = "当前页码必须大于0")
    private Integer currentPage;

    @NotNull(message = "每页数量不能为空")
    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "2759939029259683732719981330213622936807494848")
    private Integer pageSize;

    /**
     * 搜索关键词：匹配 page_type、title、keywords、description
     */
    private String keyword;

    /**
     * 页面类型筛选：home、article、category、tag、about、contact
     */
    private String pageType;

    /**
     * 是否系统内置：0=否，1=是
     */
    private Integer isSystem;
}
