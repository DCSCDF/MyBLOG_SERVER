/*
 * [UserController.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/1/14 03:54
 */

package com.jiuliu.myblog_dev.controller.user;


public class UserController {
//    @RestController
//    @RequestMapping("/api/users")
//    @PreAuthorize("hasRole('ADMIN')")  // 需要管理员权限
//    public class UserController {
//
//        /**
//         * 获取用户列表
//         * GET /api/users
//         * 权限：system:user:list
//         */
//        @GetMapping
//        @CheckPermission("system:user:list")
//        public Result listUsers(
//                @RequestParam(defaultValue = "1") Integer page,
//                @RequestParam(defaultValue = "10") Integer size,
//                @RequestParam(required = false) String username,
//                @RequestParam(required = false) Integer status) {
//            // 分页查询用户
//        }
//
//        /**
//         * 获取用户详情
//         * GET /api/users/{id}
//         * 权限：system:user:list
//         */
//        @GetMapping("/{id}")
//        @CheckPermission("system:user:list")
//        public Result getUserById(@PathVariable Long id) {
//            // 获取用户详情
//        }
//
//        /**
//         * 创建用户
//         * POST /api/users
//         * 权限：system:user:create
//         */
//        @PostMapping
//        @CheckPermission("system:user:create")
//        public Result createUser(@RequestBody @Valid UserCreateDTO dto) {
//            // 创建用户
//        }
//
//        /**
//         * 更新用户
//         * PUT /api/users/{id}
//         * 权限：system:user:edit
//         */
//        @PutMapping("/{id}")
//        @CheckPermission("system:user:edit")
//        public Result updateUser(@PathVariable Long id, @RequestBody @Valid UserUpdateDTO dto) {
//            // 更新用户
//        }
//
//        /**
//         * 删除用户
//         * DELETE /api/users/{id}
//         * 权限：system:user:delete
//         */
//        @DeleteMapping("/{id}")
//        @CheckPermission("system:user:delete")
//        public Result deleteUser(@PathVariable Long id) {
//            // 删除用户
//        }
//
//        /**
//         * 启用/禁用用户
//         * PUT /api/users/{id}/status
//         * 权限：system:user:edit
//         */
//        @PutMapping("/{id}/status")
//        @CheckPermission("system:user:edit")
//        public Result updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
//            // 更新用户状态
//        }
//    }


//    /**
//     * 用户角色管理API
//     */
//    @RestController
//    @RequestMapping("/api/users/{userId}/roles")
//    @PreAuthorize("hasRole('ADMIN')")
//    public class UserRoleController {
//
//        /**
//         * 获取用户的角色列表
//         * GET /api/users/{userId}/roles
//         * 权限：system:user:assignRole
//         */
//        @GetMapping
//        @CheckPermission("system:user:assignRole")
//        public Result getUserRoles(@PathVariable Long userId) {
//            // 获取用户所有角色
//        }
//
//        /**
//         * 为用户分配角色
//         * POST /api/users/{userId}/roles
//         * 权限：system:user:assignRole
//         */
//        @PostMapping
//        @CheckPermission("system:user:assignRole")
//        public Result assignRoles(@PathVariable Long userId, @RequestBody List<Long> roleIds) {
//            // 为用户分配角色
//        }
//
//        /**
//         * 移除用户的角色
//         * DELETE /api/users/{userId}/roles/{roleId}
//         * 权限：system:user:assignRole
//         */
//        @DeleteMapping("/{roleId}")
//        @CheckPermission("system:user:assignRole")
//        public Result removeRole(@PathVariable Long userId, @PathVariable Long roleId) {
//            // 移除用户角色
//        }
//    }


}
