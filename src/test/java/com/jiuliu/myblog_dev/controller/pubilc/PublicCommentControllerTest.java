/*
 * [PublicCommentControllerTest.java]
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

package com.jiuliu.myblog_dev.controller.pubilc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiuliu.myblog_dev.dto.comment.PublicCommentCreateDTO;
import com.jiuliu.myblog_dev.entity.blog.SysBlog;
import com.jiuliu.myblog_dev.entity.blog.comment.SysComment;
import com.jiuliu.myblog_dev.mapper.blog.SysBlogMapper;
import com.jiuliu.myblog_dev.mapper.blog.comment.SysCommentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 公共评论接口集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysBlogMapper blogMapper;

    @Autowired
    private SysCommentMapper commentMapper;


    private SysBlog testBlog;

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
    }

    @Nested
    @DisplayName("游客评论接口测试")
    class VisitorCommentApiTest {

        @Test
        @DisplayName("POST /api/public/comment - 游客提交评论成功")
        void visitorSubmitComment_Success() throws Exception {
            PublicCommentCreateDTO dto = new PublicCommentCreateDTO();
            dto.setBlogId(testBlog.getId());
            dto.setParentId(0L);
            dto.setUsername("游客张三");
            dto.setEmail("zhangsan@example.com");
            dto.setContent("这是一条游客评论");
            dto.setAvatarUrl("https://example.com/avatar.jpg");
            dto.setWebsite("https://visitor.com");

            mockMvc.perform(post("/api/public/comment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto))
                            .header("User-Agent", "Mozilla/5.0 Test Browser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.message").value("评论提交成功，待审核后显示"));
        }

        @Test
        @DisplayName("POST /api/public/comment - 缺少必填参数应返回400")
        void submitComment_MissingRequiredParam_BadRequest() throws Exception {
            PublicCommentCreateDTO dto = new PublicCommentCreateDTO();
            dto.setBlogId(testBlog.getId());
            // 缺少 parentId, username, content

            mockMvc.perform(post("/api/public/comment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("POST /api/public/comment - 内容为空应返回400")
        void submitComment_EmptyContent_BadRequest() throws Exception {
            PublicCommentCreateDTO dto = new PublicCommentCreateDTO();
            dto.setBlogId(testBlog.getId());
            dto.setParentId(0L);
            dto.setUsername("游客");
            dto.setContent("");

            mockMvc.perform(post("/api/public/comment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("文章验证接口测试")
    class ArticleValidationApiTest {

        @Test
        @DisplayName("POST /api/public/comment - 文章不存在返回404")
        void submitComment_ArticleNotFound_404() throws Exception {
            PublicCommentCreateDTO dto = new PublicCommentCreateDTO();
            dto.setBlogId(99999L);
            dto.setParentId(0L);
            dto.setUsername("游客");
            dto.setContent("评论内容");

            mockMvc.perform(post("/api/public/comment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(404))
                    .andExpect(jsonPath("$.errorMsg").value("文章不存在或已下架"));
        }
    }

    @Nested
    @DisplayName("回复评论接口测试")
    class ReplyCommentApiTest {

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
        @DisplayName("POST /api/public/comment - 回复评论成功")
        void replyToComment_Success() throws Exception {
            PublicCommentCreateDTO dto = new PublicCommentCreateDTO();
            dto.setBlogId(testBlog.getId());
            dto.setParentId(parentComment.getId());
            dto.setUsername("回复者");
            dto.setContent("这是一条回复评论");

            mockMvc.perform(post("/api/public/comment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").exists());
        }

        @Test
        @DisplayName("POST /api/public/comment - 回复不存在的父评论返回400")
        void replyToNonExistentParent_400() throws Exception {
            PublicCommentCreateDTO dto = new PublicCommentCreateDTO();
            dto.setBlogId(testBlog.getId());
            dto.setParentId(99999L);
            dto.setUsername("回复者");
            dto.setContent("回复内容");

            mockMvc.perform(post("/api/public/comment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.errorMsg").value("父评论不存在或不属于该文章"));
        }
    }

    @Nested
    @DisplayName("IP和设备信息测试")
    class IpAndDeviceInfoApiTest {

        @Test
        @DisplayName("POST /api/public/comment - 验证IP和设备信息被记录")
        void verifyIpAndDeviceInfoRecorded() throws Exception {
            PublicCommentCreateDTO dto = new PublicCommentCreateDTO();
            dto.setBlogId(testBlog.getId());
            dto.setParentId(0L);
            dto.setUsername("测试用户");
            dto.setContent("测试IP记录");

            MvcResult result = mockMvc.perform(post("/api/public/comment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto))
                            .header("User-Agent", "TestBrowser/1.0")
                            .header("X-Forwarded-For", "203.0.113.50"))
                    .andExpect(status().isOk())
                    .andReturn();

            // 验证数据库中的记录
            String responseJson = result.getResponse().getContentAsString();
            Long commentId = objectMapper.readTree(responseJson).path("data").path("id").asLong();

            SysComment savedComment = commentMapper.selectById(commentId);
            org.junit.jupiter.api.Assertions.assertNotNull(savedComment);
            org.junit.jupiter.api.Assertions.assertEquals("203.0.113.50", savedComment.getIpAddress());
            org.junit.jupiter.api.Assertions.assertEquals("TestBrowser/1.0", savedComment.getDeviceInfo());
        }
    }
}
