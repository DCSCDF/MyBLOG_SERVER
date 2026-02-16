/*
 * [PermissionController.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/1/24 23:34
 */

package com.jiuliu.myblog_dev.controller.user.permission;


import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.user.permission.PagePermissionDTO;
import com.jiuliu.myblog_dev.dto.user.permission.PagePermissionResponseDTO;
import com.jiuliu.myblog_dev.service.user.permission.PermissionService;
import com.jiuliu.myblog_dev.utils.ResponseUtil;
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
    @SaCheckPermission("system:permission")
    @PostMapping("/listAll")
    public Response<PagePermissionResponseDTO> getPagePermissions(@Valid @RequestBody PagePermissionDTO pageDto) {
        SaResult saResult = permissionService.getPagePermissions(pageDto);
        if (saResult.getCode() == 200) {
            return ResponseUtil.success((PagePermissionResponseDTO) saResult.getData(), 200);
        } else {
            return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
        }
    }

//    @RestController
//    @RequestMapping("/api/permissions")
//    @PreAuthorize("hasRole('ADMIN')")
//    public class PermissionController {
//
//        /**
//         * 获取权限列表（树形结构）
//         * GET /api/permissions
//         * 权限：system:permission:list
//         */
//        @GetMapping
//        @CheckPermission("system:permission:list")
//        public Result listPermissions() {
//            // 返回权限树
//        }
//
//        /**
//         * 获取权限详情
//         * GET /api/permissions/{id}
//         * 权限：system:permission:list
//         */
//        @GetMapping("/{id}")
//        @CheckPermission("system:permission:list")
//        public Result getPermissionById(@PathVariable Long id) {
//            // 获取权限详情
//        }
//
//        /**
//         * 创建权限
//         * POST /api/permissions
//         * 权限：system:permission:create
//         */
//        @PostMapping
//        @CheckPermission("system:permission:create")
//        public Result createPermission(@RequestBody @Valid PermissionDTO dto) {
//            // 创建权限
//        }
//
//        /**
//         * 更新权限
//         * PUT /api/permissions/{id}
//         * 权限：system:permission:edit
//         */
//        @PutMapping("/{id}")
//        @CheckPermission("system:permission:edit")
//        public Result updatePermission(@PathVariable Long id, @RequestBody @Valid PermissionDTO dto) {
//            // 更新权限
//        }
//
//        /**
//         * 删除权限
//         * DELETE /api/permissions/{id}
//         * 权限：system:permission:delete
//         */
//        @DeleteMapping("/{id}")
//        @CheckPermission("system:permission:delete")
//        public Result deletePermission(@PathVariable Long id) {
//            // 删除权限
//        }
//    }
}
