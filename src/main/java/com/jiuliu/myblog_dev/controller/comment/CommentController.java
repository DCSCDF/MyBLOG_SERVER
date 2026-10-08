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

import com.jiuliu.myblog_dev.utils.security.SecurityUtils;

import com.jiuliu.myblog_dev.dto.ServiceResult;

import org.springframework.security.access.prepost.PreAuthorize;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.comment.CommentUpdateDTO;
import com.jiuliu.myblog_dev.dto.comment.PageCommentDTO;
import com.jiuliu.myblog_dev.dto.comment.PageCommentResponseDTO;
import com.jiuliu.myblog_dev.dto.comment.PublicCommentResponseDTO;
import com.jiuliu.myblog_dev.service.comment.CommentService;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comment")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 通用方法：处理ServiceResult结果
     */
    private <T> Response<T> handleServiceResult(ServiceResult serviceResult) {
        if (serviceResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            T data = (T) serviceResult.getData();
            return ResponseUtil.success(data, 200);
        } else {
            return ResponseUtil.fail(serviceResult.getMsg(), serviceResult.getCode());
        }
    }

    /**
     * 分页获取当前用户的评论列表
     * POST /api/comment/list
     * 权限：comment:list
     */
    @PostMapping("/list")
    @PreAuthorize("hasAuthority('comment:list')")
    public Response<PageCommentResponseDTO> getPageUserComments(@Valid @RequestBody PageCommentDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ServiceResult serviceResult = commentService.getPageUserComments(dto, currentUserId);
        return handleServiceResult(serviceResult);
    }

    /**
     * 更新自己的评论
     * PUT /api/comment/{id}
     * 权限：comment:edit
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('comment:edit')")
    public Response<Object> updateComment(@PathVariable Long id,
                                          @Valid @RequestBody CommentUpdateDTO dto) {
        dto.setId(id);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ServiceResult serviceResult = commentService.updateComment(dto, currentUserId);
        return handleServiceResult(serviceResult);
    }

    /**
     * 删除自己的评论
     * DELETE /api/comment/{id}
     * 权限：comment:delete
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('comment:delete')")
    public Response<Object> deleteComment(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ServiceResult serviceResult = commentService.deleteComment(id, currentUserId);
        return handleServiceResult(serviceResult);
    }

    /**
     * 获取当前用户收到的回复评论列表
     * GET /api/comment/replies?limit=N
     * 权限：comment:list
     * 
     * @param limit 返回条数，最大100条，默认10条
     */
    @GetMapping("/replies")
    @PreAuthorize("hasAuthority('comment:list')")
    public Response<List<PublicCommentResponseDTO>> getReplyComments(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ServiceResult serviceResult = commentService.getReplyComments(currentUserId, limit);
        return handleServiceResult(serviceResult);
    }
}
