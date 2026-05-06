package com.cc91.controller;

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
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminContentController HTTP endpoint tests
 * Tests admin-only content moderation endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AdminContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        testUser = userRepository.save(testUser);
    }

    // ==================== GET /api/admin/posts ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Transactional
    void getPostsByStatus_AsAdmin_Returns200() throws Exception {
        // Arrange
        Post post1 = new Post("Title1", "Content1", testUser.getId());
        post1.setStatus("PUBLISHED");
        Post post2 = new Post("Title2", "Content2", testUser.getId());
        post2.setStatus("PENDING");
        postRepository.save(post1);
        postRepository.save(post2);

        // Act & Assert
        mockMvc.perform(get("/api/admin/posts?status=PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Title1"));
    }

    @Test
    @WithMockUser(username = "user")
    void getPostsByStatus_AsRegularUser_Returns403() throws Exception {
        mockMvc.perform(get("/api/admin/posts"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/admin/posts/{id}/status ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Transactional
    void updatePostStatus_AsAdmin_Returns200() throws Exception {
        // Arrange
        Post post = new Post("Title", "Content", testUser.getId());
        post.setStatus("PENDING");
        post = postRepository.save(post);

        String requestBody = """
            {
                "status": "PUBLISHED"
            }
            """;

        // Act & Assert
        mockMvc.perform(put("/api/admin/posts/" + post.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("帖子状态已更新"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updatePostStatus_PostNotExists_Returns404() throws Exception {
        String requestBody = """
            {
                "status": "PUBLISHED"
            }
            """;

        mockMvc.perform(put("/api/admin/posts/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("帖子不存在"));
    }

    @Test
    @WithMockUser(username = "user")
    @Transactional
    void updatePostStatus_AsRegularUser_Returns403() throws Exception {
        Post post = new Post("Title", "Content", testUser.getId());
        post = postRepository.save(post);

        String requestBody = """
            {
                "status": "PUBLISHED"
            }
            """;

        mockMvc.perform(put("/api/admin/posts/" + post.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/admin/posts/{id} ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Transactional
    void forceDeletePost_AsAdmin_Returns200() throws Exception {
        // Arrange
        Post post = new Post("Title", "Content", testUser.getId());
        post = postRepository.save(post);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/posts/" + post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("帖子已删除"));
    }

    @Test
    @WithMockUser(username = "user")
    @Transactional
    void forceDeletePost_AsRegularUser_Returns403() throws Exception {
        Post post = new Post("Title", "Content", testUser.getId());
        post = postRepository.save(post);

        mockMvc.perform(delete("/api/admin/posts/" + post.getId()))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/admin/comments/{id} ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Transactional
    void forceDeleteComment_AsAdmin_Returns200() throws Exception {
        // Arrange
        Post post = new Post("Title", "Content", testUser.getId());
        post = postRepository.save(post);

        com.cc91.entity.Comment comment = new com.cc91.entity.Comment(
                post.getId(),
                testUser.getId(),
                "Comment content",
                null
        );
        comment = commentRepository.save(comment);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/comments/" + comment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("评论已删除"));
    }

    @Test
    @WithMockUser(username = "user")
    @Transactional
    void forceDeleteComment_AsRegularUser_Returns403() throws Exception {
        Post post = new Post("Title", "Content", testUser.getId());
        post = postRepository.save(post);

        com.cc91.entity.Comment comment = new com.cc91.entity.Comment(
                post.getId(),
                testUser.getId(),
                "Comment content",
                null
        );
        comment = commentRepository.save(comment);

        mockMvc.perform(delete("/api/admin/comments/" + comment.getId()))
                .andExpect(status().isForbidden());
    }
}
