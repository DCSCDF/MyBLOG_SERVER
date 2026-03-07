/*
 * [CommentController.java]
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

package com.jiuliu.myblog_dev.controller.comment;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.comment.CommentUpdateDTO;
import com.jiuliu.myblog_dev.dto.comment.PageCommentDTO;
import com.jiuliu.myblog_dev.dto.comment.PageCommentResponseDTO;
import com.jiuliu.myblog_dev.service.comment.CommentService;
import com.jiuliu.myblog_dev.utils.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comment")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 通用方法：处理SaResult结果
     */
    private <T> Response<T> handleSaResult(SaResult saResult) {
        if (saResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            T data = (T) saResult.getData();
            return ResponseUtil.success(data, 200);
        } else {
            return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
        }
    }

    /**
     * 分页获取当前用户的评论列表
     * POST /api/comment/list
     * 权限：comment:list
     */
    @PostMapping("/list")
    @SaCheckPermission("comment:list")
    public Response<PageCommentResponseDTO> getPageUserComments(@Valid @RequestBody PageCommentDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        SaResult saResult = commentService.getPageUserComments(dto, currentUserId);
        return handleSaResult(saResult);
    }

    /**
     * 更新自己的评论
     * PUT /api/comment/{id}
     * 权限：comment:edit
     */
    @PutMapping("/{id}")
    @SaCheckPermission("comment:edit")
    public Response<Object> updateComment(@PathVariable Long id,
                                           @Valid @RequestBody CommentUpdateDTO dto) {
        dto.setId(id);
        Long currentUserId = StpUtil.getLoginIdAsLong();
        SaResult saResult = commentService.updateComment(dto, currentUserId);
        return handleSaResult(saResult);
    }

    /**
     * 删除自己的评论
     * DELETE /api/comment/{id}
     * 权限：comment:delete
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("comment:delete")
    public Response<Object> deleteComment(@PathVariable Long id) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        SaResult saResult = commentService.deleteComment(id, currentUserId);
        return handleSaResult(saResult);
    }
}
