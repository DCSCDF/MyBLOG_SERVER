/*
 * [RoleController.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/1/17 13:51
 */

package com.jiuliu.myblog_dev.controller.user.role;

public class RoleController {
//    @RestController
//    @RequestMapping("/api/roles")
//    @PreAuthorize("hasRole('ADMIN')")
//    public class RoleController {
//
//        /**
//         * 获取角色列表
//         * GET /api/roles
//         * 权限：system:role:list
//         */
//        @GetMapping
//        @CheckPermission("system:role:list")
//        public Result listRoles(
//                @RequestParam(defaultValue = "1") Integer page,
//                @RequestParam(defaultValue = "10") Integer size) {
//            // 分页查询角色
//        }
//
//        /**
//         * 获取角色详情
//         * GET /api/roles/{id}
//         * 权限：system:role:list
//         */
//        @GetMapping("/{id}")
//        @CheckPermission("system:role:list")
//        public Result getRoleById(@PathVariable Long id) {
//            // 获取角色详情
//        }
//
//        /**
//         * 创建角色
//         * POST /api/roles
//         * 权限：system:role:create
//         */
//        @PostMapping
//        @CheckPermission("system:role:create")
//        public Result createRole(@RequestBody @Valid RoleDTO dto) {
//            // 创建角色
//        }
//
//        /**
//         * 更新角色
//         * PUT /api/roles/{id}
//         * 权限：system:role:edit
//         */
//        @PutMapping("/{id}")
//        @CheckPermission("system:role:edit")
//        public Result updateRole(@PathVariable Long id, @RequestBody @Valid RoleDTO dto) {
//            // 更新角色
//        }
//
//        /**
//         * 删除角色
//         * DELETE /api/roles/{id}
//         * 权限：system:role:delete
//         */
//        @DeleteMapping("/{id}")
//        @CheckPermission("system:role:delete")
//        public Result deleteRole(@PathVariable Long id) {
//            // 删除角色（系统内置角色不能删除）
//        }
//
//        /**
//         * 启用/禁用角色
//         * PUT /api/roles/{id}/status
//         * 权限：system:role:edit
//         */
//        @PutMapping("/{id}/status")
//        @CheckPermission("system:role:edit")
//        public Result updateRoleStatus(@PathVariable Long id, @RequestParam Integer status) {
//            // 更新角色状态
//        }
//    }


//    /**
//     * 角色权限管理API
//     */
//    @RestController
//    @RequestMapping("/api/roles/{roleId}/permissions")
//    @PreAuthorize("hasRole('ADMIN')")
//    public class RolePermissionController {
//
//        /**
//         * 获取角色的权限列表
//         * GET /api/roles/{roleId}/permissions
//         * 权限：system:role:assignPermission
//         */
//        @GetMapping
//        @CheckPermission("system:role:assignPermission")
//        public Result getRolePermissions(@PathVariable Long roleId) {
//            // 获取角色的所有权限
//        }
//
//        /**
//         * 为角色分配权限
//         * POST /api/roles/{roleId}/permissions
//         * 权限：system:role:assignPermission
//         */
//        @PostMapping
//        @CheckPermission("system:role:assignPermission")
//        public Result assignPermissions(@PathVariable Long roleId, @RequestBody List<Long> permissionIds) {
//            // 为角色分配权限
//        }
//
//        /**
//         * 移除角色的权限
//         * DELETE /api/roles/{roleId}/permissions/{permissionId}
//         * 权限：system:role:assignPermission
//         */
//        @DeleteMapping("/{permissionId}")
//        @CheckPermission("system:role:assignPermission")
//        public Result removePermission(@PathVariable Long roleId, @PathVariable Long permissionId) {
//            // 移除角色权限
//        }
//    }
}
