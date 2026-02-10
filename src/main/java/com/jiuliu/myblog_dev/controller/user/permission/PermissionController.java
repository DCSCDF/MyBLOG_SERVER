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
import com.jiuliu.myblog_dev.service.user.permission.permissionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/permission")
public class PermissionController {

    private final permissionService permissionService;

    // 构造函数注入
    public PermissionController(permissionService permissionService) {
        this.permissionService = permissionService;
    }


    /**
     * 获取所有权限列表，按sort_order排序
     * POST /api/permission/listAll
     * 权限：system:permission:list
     */
    @SaCheckPermission("system:permission:list")
    @PostMapping("/listAll")
    public SaResult getAllPermissions() {
        return permissionService.getAllPermissions();
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
