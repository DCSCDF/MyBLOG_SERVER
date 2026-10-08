/*
 * [FriendLinkController.java]
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

package com.jiuliu.myblog_dev.controller.link;

import com.jiuliu.myblog_dev.dto.ServiceResult;

import org.springframework.security.access.prepost.PreAuthorize;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.link.*;
import com.jiuliu.myblog_dev.service.link.FriendLinkService;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/friend-link")
public class FriendLinkController {

    private final FriendLinkService friendLinkService;

    public FriendLinkController(FriendLinkService friendLinkService) {
        this.friendLinkService = friendLinkService;
    }

    /**
     * 分页获取外链列表
     * 权限：links:list
     */
    @PreAuthorize("hasAuthority('links:list')")
    @PostMapping("/list")
    public Response<PageFriendLinkResponseDTO> getPageFriendLinks(@Valid @RequestBody PageFriendLinkDTO pageDto) {
        ServiceResult serviceResult = friendLinkService.getPageFriendLinks(pageDto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 创建外链（默认审核通过）
     * 权限：links:create
     */
    @PreAuthorize("hasAuthority('links:create')")
    @PostMapping
    public Response<FriendLinkResponseDTO> createFriendLink(@Valid @RequestBody FriendLinkCreateDTO dto) {
        ServiceResult serviceResult = friendLinkService.createFriendLink(dto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 修改外链
     * 权限：links:edit
     */
    @PreAuthorize("hasAuthority('links:edit')")
    @PutMapping("/{id}")
    public Response<FriendLinkResponseDTO> updateFriendLink(@PathVariable Long id,
                                                            @Valid @RequestBody FriendLinkUpdateDTO dto) {
        dto.setId(id);
        ServiceResult serviceResult = friendLinkService.updateFriendLink(dto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 变更友链审核状态：待审核→通过/拒绝，通过↔拒绝
     * 请求体示例：{ "status": 1 }（0=待审核，1=已通过，2=已拒绝）
     * 权限：links:edit
     */
    @PreAuthorize("hasAuthority('links:edit')")
    @PutMapping("/{id}/status")
    public Response<FriendLinkResponseDTO> updateFriendLinkStatus(@PathVariable Long id,
                                                                  @Valid @RequestBody FriendLinkStatusUpdateDTO dto) {
        ServiceResult serviceResult = friendLinkService.updateFriendLinkStatus(id, dto);
        return handleServiceResult(serviceResult);
    }

    /**
     * 删除外链（逻辑删除）
     * 权限：links:delete
     */
    @PreAuthorize("hasAuthority('links:delete')")
    @DeleteMapping("/{id}")
    public Response<Object> deleteFriendLink(@PathVariable Long id) {
        ServiceResult serviceResult = friendLinkService.deleteFriendLink(id);
        return handleServiceResult(serviceResult);
    }

    private <T> Response<T> handleServiceResult(ServiceResult serviceResult) {
        if (serviceResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            T data = (T) serviceResult.getData();
            return ResponseUtil.success(data, 200);
        }
        return ResponseUtil.fail(serviceResult.getMsg(), serviceResult.getCode());
    }
}

