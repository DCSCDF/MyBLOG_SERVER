package com.jiuliu.myblog_dev.controller.blog;

public class BlogController {
//    @RestController
//    @RequestMapping("/api/blogs")
//    public class BlogController {
//
//        /**
//         * 获取博客列表
//         * GET /api/blogs
//         * 权限：article:list
//         */
//        @GetMapping
//        @CheckPermission("article:list")
//        public Result listBlogs(
//                @RequestParam(defaultValue = "1") Integer page,
//                @RequestParam(defaultValue = "10") Integer size,
//                @RequestParam(required = false) Long categoryId,
//                @RequestParam(required = false) String keyword) {
//            // 分页查询博客
//        }
// 
//        /**
//         * 获取博客详情
//         * GET /api/blogs/{id}
//         * 权限：article:list
//         */
//        @GetMapping("/{id}")
//        @CheckPermission("article:list")
//        public Result getBlogById(@PathVariable Long id) {
//            // 获取博客详情
//        }
//
//        /**
//         * 创建博客
//         * POST /api/blogs
//         * 权限：article:create
//         */
//        @PostMapping
//        @PreAuthorize("hasRole('ADMIN') or hasRole('AUTHOR')")
//        @CheckPermission("article:create")
//        public Result createBlog(@RequestBody @Valid BlogDTO dto) {
//            // 创建博客
//        }
//
//        /**
//         * 更新博客
//         * PUT /api/blogs/{id}
//         * 权限：article:edit
//         */
//        @PutMapping("/{id}")
//        @PreAuthorize("hasRole('ADMIN') or hasRole('AUTHOR')")
//        @CheckPermission("article:edit")
//        public Result updateBlog(@PathVariable Long id, @RequestBody @Valid BlogDTO dto) {
//            // 更新博客（作者只能更新自己的博客）
//        }
//
//        /**
//         * 删除博客
//         * DELETE /api/blogs/{id}
//         * 权限：article:delete
//         */
//        @DeleteMapping("/{id}")
//        @PreAuthorize("hasRole('ADMIN') or hasRole('AUTHOR')")
//        @CheckPermission("article:delete")
//        public Result deleteBlog(@PathVariable Long id) {
//            // 删除博客（作者只能删除自己的博客）
//        }
//
//        /**
//         * 发布博客
//         * PUT /api/blogs/{id}/publish
//         * 权限：article:publish
//         */
//        @PutMapping("/{id}/publish")
//        @PreAuthorize("hasRole('ADMIN') or hasRole('AUTHOR')")
//        @CheckPermission("article:publish")
//        public Result publishBlog(@PathVariable Long id) {
//            // 发布博客
//        }
//    }
}
