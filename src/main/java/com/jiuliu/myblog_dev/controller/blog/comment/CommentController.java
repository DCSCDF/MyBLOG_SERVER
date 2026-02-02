/*
 * [CommentController.java]
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

package com.jiuliu.myblog_dev.controller.blog.comment;

public class CommentController {
//    @RestController
//    @RequestMapping("/api/comments")
//    public class CommentController {
//
//        /**
//         * 获取评论列表
//         * GET /api/comments
//         * 权限：comment:list
//         */
//        @GetMapping
//        @CheckPermission("comment:list")
//        public Result listComments(
//                @RequestParam(defaultValue = "1") Integer page,
//                @RequestParam(defaultValue = "10") Integer size,
//                @RequestParam Long blogId) {
//            // 分页查询评论
//        }
//
//        /**
//         * 创建评论
//         * POST /api/comments
//         * 权限：comment:create
//         */
//        @PostMapping
//        @CheckPermission("comment:create")
//        public Result createComment(@RequestBody @Valid CommentDTO dto) {
//            // 创建评论
//        }
//
//        /**
//         * 更新评论
//         * PUT /api/comments/{id}
//         * 权限：comment:edit
//         */
//        @PutMapping("/{id}")
//        @PreAuthorize("hasRole('ADMIN')")
//        @CheckPermission("comment:edit")
//        public Result updateComment(@PathVariable Long id, @RequestBody @Valid CommentUpdateDTO dto) {
//            // 更新评论
//        }
//
//        /**
//         * 删除评论
//         * DELETE /api/comments/{id}
//         * 权限：comment:delete
//         */
//        @DeleteMapping("/{id}")
//        @PreAuthorize("hasRole('ADMIN')")
//        @CheckPermission("comment:delete")
//        public Result deleteComment(@PathVariable Long id) {
//            // 删除评论
//        }
//
//        /**
//         * 审核评论
//         * PUT /api/comments/{id}/approve
//         * 权限：comment:approve
//         */
//        @PutMapping("/{id}/approve")
//        @PreAuthorize("hasRole('ADMIN')")
//        @CheckPermission("comment:approve")
//        public Result approveComment(@PathVariable Long id, @RequestParam Integer status) {
//            // 审核评论
//        }
//    }
}
