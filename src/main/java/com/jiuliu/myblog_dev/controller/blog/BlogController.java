/*
 * [BlogController.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/5
 */

package com.jiuliu.myblog_dev.controller.blog;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.Response;
import com.jiuliu.myblog_dev.dto.blog.*;
import com.jiuliu.myblog_dev.service.blog.BlogService;
import com.jiuliu.myblog_dev.utils.rateLimit.RateLimit;
import com.jiuliu.myblog_dev.utils.response.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/blogs")
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    /**
     * 处理返回Map类型的SaResult结果
     *
     * @param saResult Sa-Token返回的结果
     * @return 统一响应格式
     */
    private Response<Map<String, Object>> handleSaResult(SaResult saResult) {
        if (saResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) saResult.getData();
            return ResponseUtil.success(data, 200);
        } else {
            return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
        }
    }

    /**
     * 通用方法：处理SaResult结果
     */
    private <T> Response<T> handleSaResultGeneral(SaResult saResult) {
        if (saResult.getCode() == 200) {
            @SuppressWarnings("unchecked")
            T data = (T) saResult.getData();
            return ResponseUtil.success(data, 200);
        } else {
            return ResponseUtil.fail(saResult.getMsg(), saResult.getCode());
        }
    }

    /**
     * 创建文章
     * POST /api/blogs
     * 权限：article:create
     */
    @PostMapping
    @SaCheckPermission("article:create")
    @RateLimit(count = 20, period = 60)
    public Response<Map<String, Object>> createBlog(@Valid @RequestBody BlogCreateDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return handleSaResult(blogService.createBlog(dto, currentUserId));
    }

    /**
     * 分页获取当前用户的文章列表
     * POST /api/blogs/list
     * 权限：article:list
     */
    @PostMapping("/list")
    @SaCheckPermission("article:list")
    public Response<PageUserBlogResponseDTO> getPageUserBlogs(@Valid @RequestBody PageUserBlogDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return handleSaResultGeneral(blogService.getPageUserBlogs(dto, currentUserId));
    }

    /**
     * 获取文章详情
     * GET /api/blogs/{id}
     * 权限：article:list
     */
    @GetMapping("/{id}")
    @SaCheckPermission("article:list")
    public Response<BlogDetailResponseDTO> getBlogDetail(@PathVariable Long id) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return handleSaResultGeneral(blogService.getBlogDetail(id, currentUserId));
    }

    /**
     * 更新文章状态（隐藏）
     * PUT /api/blogs/{id}/status
     * 权限：article:edit
     */
    @PutMapping("/{id}/status")
    @SaCheckPermission("article:edit")
    public Response<Map<String, Object>> updateBlogStatus(@PathVariable Long id,
                                                          @Valid @RequestBody BlogStatusUpdateDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return handleSaResult(blogService.updateBlogStatus(id, dto, currentUserId));
    }

    /**
     * 更新文章内容
     * PUT /api/blogs/{id}
     * 权限：article:edit
     */
    @PutMapping("/{id}")
    @SaCheckPermission("article:edit")
    public Response<Map<String, Object>> updateBlogContent(@PathVariable Long id,
                                                           @Valid @RequestBody BlogContentUpdateDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return handleSaResult(blogService.updateBlogContent(id, dto, currentUserId));
    }

    /**
     * 删除文章（逻辑删除）
     * DELETE /api/blogs/{id}
     * 权限：article:delete
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("article:delete")
    public Response<Map<String, Object>> deleteBlog(@PathVariable Long id) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return handleSaResult(blogService.deleteBlog(id, currentUserId));
    }
}
