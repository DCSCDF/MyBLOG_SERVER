/*
 * [PageUserBlogDTO.java]
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

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PageUserBlogDTO {

    @NotNull(message = "当前页码不能为空")
    @Min(value = 1, message = "当前页码必须大于0")
    private Integer currentPage;

    @NotNull(message = "每页数量不能为空")
    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer pageSize;

    /**
     * 搜索关键词：匹配文章标题
     */
    private String keyword;

    /**
     * 状态筛选：是否隐藏 (null=全部, 0=显示, 1=隐藏)
     */
    private Boolean isHidden;

    /**
     * 状态筛选：是否置顶 (null=全部, 0=不置顶, 1=置顶)
     */
    private Boolean isTop;

    /**
     * 状态筛选：是否推荐 (null=全部, 0=不推荐, 1=推荐)
     */
    private Boolean isRecommend;
}
