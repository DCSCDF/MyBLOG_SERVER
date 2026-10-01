/*
 * [PermissionController.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/18 11:52
 */

package com.jiuliu.myblog_dev.controller.user.permission;


import org.springframework.security.access.prepost.PreAuthorize;
import com.jiuliu.myblog_dev.common.ServiceResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.user.permission.PagePermissionDTO;
import com.jiuliu.myblog_dev.dto.user.permission.PagePermissionResponseDTO;
import com.jiuliu.myblog_dev.service.user.permission.PermissionService;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/permission")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }


    /**
     * 分页获取权限列表
     * POST /api/permission/page
     * 权限：system:permission
     *
     * @param pageDto 分页参数
     * @return 分页结果
     */
    @PreAuthorize("hasAuthority('system:permission')")
    @PostMapping("/listAll")
    public Response<PagePermissionResponseDTO> getPagePermissions(@Valid @RequestBody PagePermissionDTO pageDto) {
        ServiceResult serviceResult = permissionService.getPagePermissions(pageDto);
        if (serviceResult.getCode() == 200) {
            return ResponseUtil.success((PagePermissionResponseDTO) serviceResult.getData(), 200);
        } else {
            return ResponseUtil.fail(serviceResult.getMsg(), serviceResult.getCode());
        }
    }

}
