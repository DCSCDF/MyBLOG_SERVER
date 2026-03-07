/*
 * [PageGlobalCommentResponseDTO.java]
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

import com.jiuliu.myblog_dev.dto.common.FilterOptionItem;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 分页查询全局评论响应DTO
 */
@Data
public class PageGlobalCommentResponseDTO {

    private List<CommentResponseDTO> records;

    private Long total;

    private Long size;

    private Long current;

    private Long pages;

    /**
     * 可用的筛选项（如 status），供前端渲染筛选控件
     */
    private Map<String, List<FilterOptionItem>> filterOptions;
}
