/*
 * [GlobalCommentController.java]
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

import org.springframework.security.access.prepost.PreAuthorize;
import com.jiuliu.myblog_dev.common.ServiceResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.comment.*;
import com.jiuliu.myblog_dev.service.comment.GlobalCommentService;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/comment")
public class GlobalCommentController {

    private final GlobalCommentService globalCommentService;

    public GlobalCommentController(GlobalCommentService globalCommentService) {
        this.globalCommentService = globalCommentService;
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
     * 分页获取所有评论列表
     * POST /api/system/comment/list
     * 权限：system:comment:list
     */
    @PostMapping("/list")
    @PreAuthorize("hasAuthority('system:comment:list')")
    public Response<PageGlobalCommentResponseDTO> getPageGlobalComments(@Valid @RequestBody PageGlobalCommentDTO dto) {
        ServiceResult serviceResult = globalCommentService.getPageGlobalComments(dto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 更新任意评论
     * PUT /api/system/comment/{id}
     * 权限：system:comment:edit
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:comment:edit')")
    public Response<CommentResponseDTO> updateComment(@PathVariable Long id,
                                                      @Valid @RequestBody CommentUpdateDTO dto) {
        dto.setId(id);
        ServiceResult serviceResult = globalCommentService.updateComment(dto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 删除任意评论
     * DELETE /api/system/comment/{id}
     * 权限：system:comment:delete
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:comment:delete')")
    public Response<Object> deleteComment(@PathVariable Long id) {
        ServiceResult serviceResult = globalCommentService.deleteComment(id);
        return handleServiceResult(serviceResult);
    }

    /**
     * 审核评论（修改状态）
     * PUT /api/system/comment/{id}/approve
     * 权限：system:comment:approve
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('system:comment:approve')")
    public Response<CommentResponseDTO> approveComment(@PathVariable Long id,
                                                       @Valid @RequestBody CommentStatusUpdateDTO dto) {
        ServiceResult serviceResult = globalCommentService.approveComment(id, dto);
        return handleServiceResult(serviceResult);
    }
}
