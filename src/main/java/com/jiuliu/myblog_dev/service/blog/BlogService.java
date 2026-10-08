/*
 * [BlogService.java]
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

package com.jiuliu.myblog_dev.service.blog;

import com.jiuliu.myblog_dev.dto.ServiceResult;

import com.jiuliu.myblog_dev.dto.blog.*;

public interface BlogService {

    /**
     * 创建文章
     *
     * @param dto        文章创建DTO
     * @param authorId   作者ID（从token获取）
     * @return 操作结果
     */
    ServiceResult createBlog(BlogCreateDTO dto, Long authorId);

    /**
     * 分页获取用户文章列表
     *
     * @param dto       分页查询DTO
     * @param userId    当前用户ID
     * @return 分页结果
     */
    ServiceResult getPageUserBlogs(PageUserBlogDTO dto, Long userId);

    /**
     * 获取文章详情
     *
     * @param blogId   文章ID
     * @param userId   当前用户ID
     * @return 文章详情
     */
    ServiceResult getBlogDetail(Long blogId, Long userId);

    /**
     * 更新文章状态（隐藏、置顶、推荐）
     *
     * @param blogId   文章ID
     * @param dto      状态更新DTO
     * @param userId   当前用户ID
     * @return 操作结果
     */
    ServiceResult updateBlogStatus(Long blogId, BlogStatusUpdateDTO dto, Long userId);

    /**
     * 更新文章内容
     *
     * @param blogId   文章ID
     * @param dto      内容更新DTO
     * @param userId   当前用户ID
     * @return 操作结果
     */
    ServiceResult updateBlogContent(Long blogId, BlogContentUpdateDTO dto, Long userId);

    /**
     * 删除文章（逻辑删除）
     *
     * @param blogId   文章ID
     * @param userId   当前用户ID
     * @return 操作结果
     */
    ServiceResult deleteBlog(Long blogId, Long userId);
}
