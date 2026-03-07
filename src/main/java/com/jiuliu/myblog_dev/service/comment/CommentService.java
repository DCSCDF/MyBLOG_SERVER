/*
 * [CommentService.java]
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
import com.jiuliu.myblog_dev.dto.comment.CommentUpdateDTO;
import com.jiuliu.myblog_dev.dto.comment.PageCommentDTO;


/**
 * 评论服务接口（用户自己的评论）
 */
public interface CommentService {

    /**
     * 分页获取当前用户的评论列表
     */
    SaResult getPageUserComments(PageCommentDTO dto, Long userId);

    /**
     * 更新自己的评论
     */
    SaResult updateComment(CommentUpdateDTO dto, Long userId);

    /**
     * 删除自己的评论
     */
    SaResult deleteComment(Long commentId, Long userId);
}
