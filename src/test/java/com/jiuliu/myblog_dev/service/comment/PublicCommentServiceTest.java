/*
 * [PublicCommentServiceTest.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/24
 */

package com.jiuliu.myblog_dev.service.comment;

import cn.dev33.satoken.util.SaResult;
import com.jiuliu.myblog_dev.dto.comment.PublicCommentCreateDTO;
import com.jiuliu.myblog_dev.entity.blog.SysBlog;
import com.jiuliu.myblog_dev.entity.blog.comment.SysComment;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.mapper.blog.SysBlogMapper;
import com.jiuliu.myblog_dev.mapper.blog.comment.SysCommentMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 公共评论服务单元测试
 */
@SpringBootTest
@Transactional
class PublicCommentServiceTest {

    @Autowired
    private PublicCommentService publicCommentService;

    @Autowired
    private SysCommentMapper commentMapper;

    @Autowired
    private SysBlogMapper blogMapper;

    @Autowired
    private SysUserMapper userMapper;

    private SysBlog testBlog;
    private SysUser testUser;
    private SysUser adminUser;

    @BeforeEach
    void setUp() {
        // 创建测试文章（公开）
        testBlog = new SysBlog();
        testBlog.setTitle("测试文章");
        testBlog.setContent("# 测试内容");
        testBlog.setHidden(false);
        testBlog.setAuthorId(1L);
        testBlog.setCommentCount(0);
        blogMapper.insert(testBlog);

        // 创建测试用户
        testUser = new SysUser();
        testUser.setUsername("test_user");
        testUser.setNickname("测试用户");
        testUser.setEmail("test@example.com");
        testUser.setAvatarUrl("https://example.com/avatar.jpg");
        testUser.setPassword("password");
        userMapper.insert(testUser);

        // 创建管理员用户
        adminUser = new SysUser();
        adminUser.setUsername("admin_user");
        adminUser.setNickname("管理员用户");
        adminUser.setEmail("admin@example.com");
        adminUser.setAvatarUrl("https://example.com/admin.jpg");
        adminUser.setPassword("password");
        userMapper.insert(adminUser);
    }

    @Nested
    @DisplayName("游客评论测试")
    class VisitorCommentTest {

        @Test
        @DisplayName("游客提交顶级评论成功")
        void visitorSubmitTopLevelComment_Success() {
            PublicCommentCreateDTO dto = new PublicCommentCreateDTO();
            dto.setBlogId(testBlog.getId());
            dto.setParentId(0L);
            dto.setUsername("游客张三");
            dto.setEmail("zhangsan@example.com");
            dto.setContent("这是一条游客评论");
            dto.setAvatarUrl("https://example.com/visitor.jpg");
            dto.setWebsite("https://visitor.com");

            SaResult result = publicCommentService.createComment(
                    dto, "127.0.0.1", "Mozilla/5.0", false, null, false);

            assertThat(result.getCode()).isEqualTo(200);
            assertThat(result.getData()).isNotNull();

            // 验证评论已保存
            Long commentId = ((Number) ((java.util.Map<String, Object>) result.getData()).get("id")).longValue();
            SysComment savedComment = commentMapper.selectById(commentId);
            assertThat(savedComment).isNotNull();
            assertThat(savedComment.getUsername()).isEqualTo("游客张三");
            assertThat(savedComment.getEmail()).isEqualTo("zhangsan@example.com");
            assertThat(savedComment.getStatus()).isEqualTo((byte) 0); // 待审核
            assertThat(savedComment.getAdmin()).isFalse();
            assertThat(savedComment.getIpAddress()).isEqualTo("127.0.0.1");
            assertThat(savedComment.getDeviceInfo()).isEqualTo("Mozilla/5.0");

            // 验证评论数已更新
            SysBlog updatedBlog = blogMapper.selectById(testBlog.getId());
            assertThat(updatedBlog.getCommentCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("游客未填写用户名应失败")
        void visitorSubmitComment_WithoutUsername_Fail() {
            PublicCommentCreateDTO dto = new PublicCommentCreateDTO();
            dto.setBlogId(testBlog.getId());
            dto.setParentId(0L);
            dto.setContent("这是一条游客评论");

            SaResult result = publicCommentService.createComment(
                    dto, "127.0.0.1", "Mozilla/5.0", false, null, false);

            assertThat(result.getCode()).isEqualTo(400);
            assertThat(result.getMsg()).contains("评论者名称不能为空");
        }

        @Test
        @DisplayName("游客评论内容为空应失败")
        void visitorSubmitComment_EmptyContent_Fail() {
            PublicCommentCreateDTO dto = new PublicCommentCreateDTO();
            dto.setBlogId(testBlog.getId());
            dto.setParentId(0L);
            dto.setUsername("游客");
            dto.setContent("");

            SaResult result = publicCommentService.createComment(
                    dto, "127.0.0.1", "Mozilla/5.0", false, null, false);

            assertThat(result.getCode()).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("已登录用户评论测试")
    class LoggedInUserCommentTest {

        @Test
        @DisplayName("已登录用户提交评论成功，使用用户表信息")
        void loggedInUserSubmitComment_Success() {
            PublicCommentCreateDTO dto = new PublicCommentCreateDTO();
            dto.setBlogId(testBlog.getId());
            dto.setParentId(0L);
            dto.setContent("这是一条已登录用户的评论");

            SaResult result = publicCommentService.createComment(
                    dto, "192.168.1.1", "Chrome/100.0", true, testUser.getId(), false);

            assertThat(result.getCode()).isEqualTo(200);

            // 验证评论使用用户表信息
            Long commentId = ((Number) ((java.util.Map<String, Object>) result.getData()).get("id")).longValue();
            SysComment savedComment = commentMapper.selectById(commentId);
            assertThat(savedComment).isNotNull();
            assertThat(savedComment.getUserId()).isEqualTo(testUser.getId());
            assertThat(savedComment.getUsername()).isEqualTo(testUser.getNickname());
            assertThat(savedComment.getEmail()).isEqualTo(testUser.getEmail());
            assertThat(savedComment.getAvatarUrl()).isEqualTo(testUser.getAvatarUrl());
            assertThat(savedComment.getStatus()).isEqualTo((byte) 1); // 直接通过
            assertThat(savedComment.getAdmin()).isFalse();
        }

        @Test
        @DisplayName("管理员提交评论成功，is_admin为true")
        void adminSubmitComment_Success() {
            PublicCommentCreateDTO dto = new PublicCommentCreateDTO();
            dto.setBlogId(testBlog.getId());
            dto.setParentId(0L);
            dto.setContent("这是一条管理员评论");

            SaResult result = publicCommentService.createComment(
                    dto, "192.168.1.1", "Chrome/100.0", true, adminUser.getId(), true);

            assertThat(result.getCode()).isEqualTo(200);

            Long commentId = ((Number) ((java.util.Map<String, Object>) result.getData()).get("id")).longValue();
            SysComment savedComment = commentMapper.selectById(commentId);
            assertThat(savedComment).isNotNull();
            assertThat(savedComment.getAdmin()).isTrue();
        }

        @Test
        @DisplayName("已登录用户提交评论忽略传入的username")
        void loggedInUserSubmitComment_IgnoreInputUsername() {
            PublicCommentCreateDTO dto = new PublicCommentCreateDTO();
            dto.setBlogId(testBlog.getId());
            dto.setParentId(0L);
            dto.setUsername("不应该被使用的用户名"); // 应该被忽略
            dto.setEmail("should@ignore.com"); // 应该被忽略
            dto.setContent("这是一条已登录用户的评论");

            SaResult result = publicCommentService.createComment(
                    dto, "192.168.1.1", "Chrome/100.0", true, testUser.getId(), false);

            assertThat(result.getCode()).isEqualTo(200);

            Long commentId = ((Number) ((java.util.Map<String, Object>) result.getData()).get("id")).longValue();
            SysComment savedComment = commentMapper.selectById(commentId);
            assertThat(savedComment).isNotNull();
            assertThat(savedComment.getUsername()).isEqualTo(testUser.getNickname()); // 使用用户表信息
            assertThat(savedComment.getEmail()).isEqualTo(testUser.getEmail()); // 使用用户表信息
        }
    }

    @Nested
    @DisplayName("文章验证测试")
    class ArticleValidationTest {

        @Test
        @DisplayName("文章不存在应失败")
        void submitComment_ArticleNotFound_Fail() {
            PublicCommentCreateDTO dto = new PublicCommentCreateDTO();
            dto.setBlogId(99999L);
            dto.setParentId(0L);
            dto.setUsername("游客");
            dto.setContent("评论内容");

            SaResult result = publicCommentService.createComment(
                    dto, "127.0.0.1", "Mozilla/5.0", false, null, false);

            assertThat(result.getCode()).isEqualTo(404);
            assertThat(result.getMsg()).contains("文章不存在或已下架");
        }
    }

    @Nested
    @DisplayName("回复评论测试")
    class ReplyCommentTest {

        private SysComment parentComment;

        @BeforeEach
        void setUpReply() {
            // 创建一条已通过的父评论
            parentComment = new SysComment();
            parentComment.setBlogId(testBlog.getId());
            parentComment.setParentId(0L);
            parentComment.setUsername("父评论者");
            parentComment.setContent("这是父评论");
            parentComment.setStatus((byte) 1); // 已通过
            parentComment.setAdmin(false);
            commentMapper.insert(parentComment);
        }

        @Test
        @DisplayName("回复已通过的评论成功")
        void replyToApprovedComment_Success() {
            PublicCommentCreateDTO dto = new PublicCommentCreateDTO();
            dto.setBlogId(testBlog.getId());
            dto.setParentId(parentComment.getId());
            dto.setUsername("回复者");
            dto.setContent("这是一条回复");

            SaResult result = publicCommentService.createComment(
                    dto, "127.0.0.1", "Mozilla/5.0", false, null, false);

            assertThat(result.getCode()).isEqualTo(200);

            Long commentId = ((Number) ((java.util.Map<String, Object>) result.getData()).get("id")).longValue();
            SysComment savedComment = commentMapper.selectById(commentId);
            assertThat(savedComment.getParentId()).isEqualTo(parentComment.getId());
        }

        @Test
        @DisplayName("回复不存在的父评论应失败")
        void replyToNonExistentParent_Fail() {
            PublicCommentCreateDTO dto = new PublicCommentCreateDTO();
            dto.setBlogId(testBlog.getId());
            dto.setParentId(99999L);
            dto.setUsername("回复者");
            dto.setContent("这是一条回复");

            SaResult result = publicCommentService.createComment(
                    dto, "127.0.0.1", "Mozilla/5.0", false, null, false);

            assertThat(result.getCode()).isEqualTo(400);
            assertThat(result.getMsg()).contains("父评论不存在");
        }

        @Test
        @DisplayName("回复其他文章的评论应失败")
        void replyToDifferentArticleComment_Fail() {
            // 创建另一篇文章
            SysBlog anotherBlog = new SysBlog();
            anotherBlog.setTitle("另一篇文章");
            anotherBlog.setContent("# 内容");
            anotherBlog.setHidden(false);
            anotherBlog.setAuthorId(1L);
            anotherBlog.setCommentCount(0);
            blogMapper.insert(anotherBlog);

            // 创建另一篇文章的评论
            SysComment anotherBlogComment = new SysComment();
            anotherBlogComment.setBlogId(anotherBlog.getId());
            anotherBlogComment.setParentId(0L);
            anotherBlogComment.setUsername("用户");
            anotherBlogComment.setContent("另一篇文章的评论");
            anotherBlogComment.setStatus((byte) 1);
            commentMapper.insert(anotherBlogComment);

            // 尝试用testBlog的ID回复anotherBlogComment
            PublicCommentCreateDTO dto = new PublicCommentCreateDTO();
            dto.setBlogId(testBlog.getId());
            dto.setParentId(anotherBlogComment.getId());
            dto.setUsername("回复者");
            dto.setContent("回复");

            SaResult result = publicCommentService.createComment(
                    dto, "127.0.0.1", "Mozilla/5.0", false, null, false);

            assertThat(result.getCode()).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("IP和设备信息测试")
    class IpAndDeviceInfoTest {

        @Test
        @DisplayName("验证IP和设备信息正确保存")
        void verifyIpAndDeviceInfoSaved() {
            PublicCommentCreateDTO dto = new PublicCommentCreateDTO();
            dto.setBlogId(testBlog.getId());
            dto.setParentId(0L);
            dto.setUsername("测试用户");
            dto.setContent("测试IP记录");

            SaResult result = publicCommentService.createComment(
                    dto, "203.0.113.50", "TestBrowser/1.0", false, null, false);

            assertThat(result.getCode()).isEqualTo(200);

            Long commentId = ((Number) ((java.util.Map<String, Object>) result.getData()).get("id")).longValue();
            SysComment savedComment = commentMapper.selectById(commentId);
            assertThat(savedComment.getIpAddress()).isEqualTo("203.0.113.50");
            assertThat(savedComment.getDeviceInfo()).isEqualTo("TestBrowser/1.0");
        }
    }
}
