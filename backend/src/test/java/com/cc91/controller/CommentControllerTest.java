package com.cc91.controller;

import com.cc91.entity.Comment;
import com.cc91.entity.Post;
import com.cc91.entity.User;
import com.cc91.repository.CommentRepository;
import com.cc91.repository.PostRepository;
import com.cc91.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CommentController 业务逻辑测试
 * 测试评论相关的 HTTP 接口
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CommentControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @BeforeEach
    @Transactional
    void cleanDatabase() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== POST /api/posts/{postId}/comments 测试 ====================

    @Test
    @WithMockUser(username = "author")
    @Transactional
    void createComment_Authenticated_ReturnsCreatedComment() throws Exception {
        // Arrange: 创建用户和帖子
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post post = new Post("标题", "内容", user.getId());
        postRepository.saveAndFlush(post);

        String requestBody = """
            {
                "content": "这是一条评论"
            }
            """;

        // Act & Assert: 认证用户可以创建评论
        mockMvc.perform(post("/api/posts/" + post.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("评论创建成功"))
                .andExpect(jsonPath("$.data.content").value("这是一条评论"))
                .andExpect(jsonPath("$.data.authorUsername").value("author"))
                .andExpect(jsonPath("$.data.postId").value(post.getId()))
                .andExpect(jsonPath("$.data.parentId").isEmpty())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @WithMockUser(username = "nonexistent")
    void createComment_UserNotExists_Returns404() throws Exception {
        // Arrange: 创建帖子（用户不存在）
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post post = new Post("标题", "内容", user.getId());
        postRepository.saveAndFlush(post);

        String requestBody = """
            {
                "content": "评论内容"
            }
            """;

        // Act & Assert: 用户不存在返回 404
        mockMvc.perform(post("/api/posts/" + post.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    @Test
    @WithMockUser(username = "author")
    void createComment_PostNotExists_Returns404() throws Exception {
        // Arrange: 创建用户（帖子不存在）
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        String requestBody = """
            {
                "content": "评论内容"
            }
            """;

        // Act & Assert: 帖子不存在返回 404
        mockMvc.perform(post("/api/posts/999/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("帖子不存在"));
    }

    // ==================== POST /api/comments/{id}/reply 测试 ====================

    @Test
    @WithMockUser(username = "user2")
    @Transactional
    void replyToComment_Authenticated_ReturnsCreatedReply() throws Exception {
        // Arrange: 创建用户、帖子和父评论
        User user1 = new User("user1", "user1@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user1);

        User user2 = new User("user2", "user2@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user2);

        Post post = new Post("标题", "内容", user1.getId());
        postRepository.saveAndFlush(post);

        Comment parentComment = new Comment(post.getId(), user1.getId(), "父评论", null);
        commentRepository.saveAndFlush(parentComment);

        String requestBody = """
            {
                "content": "这是一条回复"
            }
            """;

        // Act & Assert: 认证用户可以回复评论
        mockMvc.perform(post("/api/comments/" + parentComment.getId() + "/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("回复成功"))
                .andExpect(jsonPath("$.data.content").value("这是一条回复"))
                .andExpect(jsonPath("$.data.authorUsername").value("user2"))
                .andExpect(jsonPath("$.data.parentId").value(parentComment.getId()))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @WithMockUser(username = "nonexistent")
    void replyToComment_UserNotExists_Returns404() throws Exception {
        // Arrange: 创建帖子和评论
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post post = new Post("标题", "内容", user.getId());
        postRepository.saveAndFlush(post);

        Comment comment = new Comment(post.getId(), user.getId(), "评论", null);
        commentRepository.saveAndFlush(comment);

        String requestBody = """
            {
                "content": "回复内容"
            }
            """;

        // Act & Assert: 用户不存在返回 404
        mockMvc.perform(post("/api/comments/" + comment.getId() + "/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    @Test
    @WithMockUser(username = "author")
    void replyToComment_ParentCommentNotExists_Returns404() throws Exception {
        // Arrange: 创建用户
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        String requestBody = """
            {
                "content": "回复内容"
            }
            """;

        // Act & Assert: 父评论不存在返回 404
        mockMvc.perform(post("/api/comments/999/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("评论不存在"));
    }

    // ==================== DELETE /api/comments/{id} 测试 ====================

    @Test
    @WithMockUser(username = "author")
    @Transactional
    void deleteComment_Authenticated_ReturnsSuccess() throws Exception {
        // Arrange: 创建用户、帖子和评论
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post post = new Post("标题", "内容", user.getId());
        postRepository.saveAndFlush(post);

        Comment comment = new Comment(post.getId(), user.getId(), "评论内容", null);
        commentRepository.saveAndFlush(comment);

        // Act & Assert: 作者可以删除评论
        mockMvc.perform(delete("/api/comments/" + comment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("评论删除成功"));
    }

    @Test
    @WithMockUser(username = "other")
    @Transactional
    void deleteComment_NotAuthor_Returns403() throws Exception {
        // Arrange: 创建两个用户
        User author = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(author);

        User otherUser = new User("other", "other@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(otherUser);

        Post post = new Post("标题", "内容", author.getId());
        postRepository.saveAndFlush(post);

        Comment comment = new Comment(post.getId(), author.getId(), "评论内容", null);
        commentRepository.saveAndFlush(comment);

        // Act & Assert: 非作者删除返回 403
        mockMvc.perform(delete("/api/comments/" + comment.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("无权限删除此评论"));
    }

    @Test
    @WithMockUser(username = "author")
    void deleteComment_CommentNotExists_Returns404() throws Exception {
        // Arrange: 创建用户
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        // Act & Assert: 评论不存在返回 404
        mockMvc.perform(delete("/api/comments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("评论不存在"));
    }

    // ==================== GET /api/posts/{postId}/comments 测试 ====================

    @Test
    @Transactional
    void getCommentsByPostId_ReturnsCommentsInTreeStructure() throws Exception {
        // Arrange: 创建用户、帖子和评论树
        User user1 = new User("user1", "user1@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user1);

        User user2 = new User("user2", "user2@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user2);

        Post post = new Post("标题", "内容", user1.getId());
        postRepository.saveAndFlush(post);

        // 创建顶级评论
        Comment comment1 = new Comment(post.getId(), user1.getId(), "评论1", null);
        commentRepository.saveAndFlush(comment1);

        Comment comment2 = new Comment(post.getId(), user2.getId(), "评论2", null);
        commentRepository.saveAndFlush(comment2);

        // 创建回复
        Comment reply1 = new Comment(post.getId(), user2.getId(), "回复1-1", comment1.getId());
        commentRepository.saveAndFlush(reply1);

        // Act & Assert: 获取评论列表，验证树形结构
        mockMvc.perform(get("/api/posts/" + post.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("评论1"))
                .andExpect(jsonPath("$[0].replies.length()").value(1))
                .andExpect(jsonPath("$[0].replies[0].content").value("回复1-1"))
                .andExpect(jsonPath("$[1].content").value("评论2"))
                .andExpect(jsonPath("$[1].replies.length()").value(0));
    }

    @Test
    void getCommentsByPostId_PostNotExists_Returns404() throws Exception {
        // Act & Assert: 帖子不存在返回 404
        mockMvc.perform(get("/api/posts/999/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("帖子不存在"));
    }

    @Test
    @Transactional
    void getCommentsByPostId_OnlyReturnsPublishedComments() throws Exception {
        // Arrange: 创建用户、帖子和不同状态的评论
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post post = new Post("标题", "内容", user.getId());
        postRepository.saveAndFlush(post);

        // 创建已发布评论
        Comment publishedComment = new Comment(post.getId(), user.getId(), "已发布评论", null);
        publishedComment.setStatus("PUBLISHED");
        commentRepository.saveAndFlush(publishedComment);

        // 创建已删除评论
        Comment deletedComment = new Comment(post.getId(), user.getId(), "已删除评论", null);
        deletedComment.setStatus("DELETED");
        commentRepository.saveAndFlush(deletedComment);

        // Act & Assert: 只返回 PUBLISHED 状态的评论
        mockMvc.perform(get("/api/posts/" + post.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("已发布评论"));
    }
}
