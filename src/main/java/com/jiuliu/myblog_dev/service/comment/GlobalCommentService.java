/*
 * [GlobalCommentService.java]
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

package com.jiuliu.myblog_dev.service.comment;

import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.comment.CommentStatusUpdateDTO;
import com.jiuliu.myblog_dev.dto.comment.CommentUpdateDTO;
import com.jiuliu.myblog_dev.dto.comment.PageGlobalCommentDTO;


/**
 * 全局评论服务接口（管理员）
 */
public interface GlobalCommentService {

    /**
     * 分页获取所有评论列表
     */
    SaResult getPageGlobalComments(PageGlobalCommentDTO dto);

    /**
     * 更新任意评论
     */
    SaResult updateComment(CommentUpdateDTO dto);

    /**
     * 删除任意评论
     */
    SaResult deleteComment(Long commentId);

    /**
     * 审核评论（修改状态）
     */
    SaResult approveComment(Long commentId, CommentStatusUpdateDTO dto);
}
