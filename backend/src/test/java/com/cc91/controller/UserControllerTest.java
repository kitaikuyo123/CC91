package com.cc91.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 业务逻辑测试
 * 测试用户资料相关的 HTTP 接口
 */
@SpringBootTest
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.cc91.repository.UserRepository userRepository;

    @Autowired
    private com.cc91.repository.UserProfileRepository userProfileRepository;

    @Autowired
    private com.cc91.repository.PostRepository postRepository;

    @Autowired
    private com.cc91.repository.CommentRepository commentRepository;

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
        userProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== GET /api/users/me 测试 ====================

    @Test
    @WithMockUser(username = "testuser")
    @Transactional
    void getMyProfile_Authenticated_ReturnsProfile() throws Exception {
        // Arrange: 创建一个用户
        com.cc91.entity.User user = new com.cc91.entity.User(
                "testuser",
                "test@example.com",
                passwordEncoder.encode("password123")
        );
        userRepository.save(user);

        // Act & Assert: 认证用户可以获取自己的资料
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.avatarUrl").isEmpty())
                .andExpect(jsonPath("$.bio").isEmpty());
    }

    // ==================== GET /api/users/{username} 测试 ====================

    @Test
    @Transactional
    void getUserProfile_UserExists_ReturnsProfile() throws Exception {
        // Arrange: 创建一个用户
        com.cc91.entity.User user = new com.cc91.entity.User(
                "targetuser",
                "target@example.com",
                passwordEncoder.encode("password123")
        );
        userRepository.save(user);

        // Act & Assert: 公开接口，任何人都可以查看用户资料
        mockMvc.perform(get("/api/users/targetuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("targetuser"))
                .andExpect(jsonPath("$.email").value("target@example.com"));
    }

    @Test
    void getUserProfile_UserNotExists_Returns400() throws Exception {
        // Act & Assert: 用户不存在时返回 400（业务异常被 GlobalExceptionHandler 捕获）
        mockMvc.perform(get("/api/users/nonexistent"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    // ==================== PUT /api/users/me/profile 测试 ====================

    @Test
    @WithMockUser(username = "testuser")
    @Transactional
    void updateProfile_Authenticated_ReturnsUpdatedProfile() throws Exception {
        // Arrange: 创建一个用户
        com.cc91.entity.User user = new com.cc91.entity.User(
                "testuser",
                "test@example.com",
                passwordEncoder.encode("password123")
        );
        userRepository.save(user);

        String requestBody = """
            {
                "avatarUrl": "https://example.com/avatar.jpg",
                "bio": "我的个人签名",
                "location": "北京",
                "website": "https://example.com"
            }
            """;

        // Act & Assert: 认证用户可以更新资料
        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.jpg"))
                .andExpect(jsonPath("$.bio").value("我的个人签名"))
                .andExpect(jsonPath("$.location").value("北京"))
                .andExpect(jsonPath("$.website").value("https://example.com"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateProfile_InvalidWebsite_Returns400() throws Exception {
        // Arrange: 准备无效 URL 的更新请求
        String requestBody = """
            {
                "website": "not-a-valid-url"
            }
            """;

        // Act & Assert: 输入验证失败返回 400
        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("输入验证失败"))
                .andExpect(jsonPath("$.data.website").value("网站地址格式不正确"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @Transactional
    void updateProfile_PartialFields_ReturnsUpdatedProfile() throws Exception {
        // Arrange: 创建一个用户并设置初始资料
        com.cc91.entity.User user = new com.cc91.entity.User(
                "testuser",
                "test@example.com",
                passwordEncoder.encode("password123")
        );
        userRepository.saveAndFlush(user);

        // 先设置一些初始值
        com.cc91.entity.UserProfile profile = new com.cc91.entity.UserProfile();
        profile.setUser(user);
        profile.setAvatarUrl("https://old.com/avatar.jpg");
        profile.setBio("旧签名");
        profile.setLocation("旧地点");
        profile.setWebsite("https://old.com");
        userProfileRepository.saveAndFlush(profile);

        // Act: 只更新部分字段
        String requestBody = """
            {
                "bio": "新签名"
            }
            """;

        // Assert: 验证只有指定字段被更新
        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("https://old.com/avatar.jpg"))
                .andExpect(jsonPath("$.bio").value("新签名"))
                .andExpect(jsonPath("$.location").value("旧地点"))
                .andExpect(jsonPath("$.website").value("https://old.com"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateProfile_UserNotExists_Returns400() throws Exception {
        // Arrange: 用户不存在（数据库中没有 testuser）
        String requestBody = """
            {
                "bio": "测试签名"
            }
            """;

        // Act & Assert: 用户不存在返回 400
        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateProfile_AvatarUrlTooLong_Returns400() throws Exception {
        // Arrange: 头像 URL 超过 500 字符
        String longUrl = "https://example.com/".repeat(50);
        String requestBody = String.format("""
            {
                "avatarUrl": "%s"
            }
            """, longUrl);

        // Act & Assert: 输入验证失败
        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("输入验证失败"))
                .andExpect(jsonPath("$.data.avatarUrl").value("头像URL不能超过500字符"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateProfile_BioTooLong_Returns400() throws Exception {
        // Arrange: 个人签名超过 500 字符
        String longBio = "a".repeat(501);
        String requestBody = String.format("""
            {
                "bio": "%s"
            }
            """, longBio);

        // Act & Assert: 输入验证失败
        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("输入验证失败"))
                .andExpect(jsonPath("$.data.bio").value("个人签名不能超过500字符"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateProfile_LocationTooLong_Returns400() throws Exception {
        // Arrange: 所在地超过 100 字符
        String longLocation = "a".repeat(101);
        String requestBody = String.format("""
            {
                "location": "%s"
            }
            """, longLocation);

        // Act & Assert: 输入验证失败
        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("输入验证失败"))
                .andExpect(jsonPath("$.data.location").value("所在地不能超过100字符"));
    }

            // ==================== GET /api/users/me/posts 测试 ====================

            @Test
            @WithMockUser(username = "testuser")
            @Transactional
            void getMyPosts_Authenticated_ReturnsOnlyMyPostsInDescOrder() throws Exception {
            // Arrange
            com.cc91.entity.User me = new com.cc91.entity.User(
                "testuser",
                "test@example.com",
                passwordEncoder.encode("password123")
            );
            userRepository.saveAndFlush(me);

            com.cc91.entity.User other = new com.cc91.entity.User(
                "other",
                "other@example.com",
                passwordEncoder.encode("password123")
            );
            userRepository.saveAndFlush(other);

            com.cc91.entity.Post oldPost = new com.cc91.entity.Post("Old Title", "Old Content", me.getId());
            oldPost.setCreatedAt(LocalDateTime.now().minusDays(2));
            postRepository.saveAndFlush(oldPost);

            com.cc91.entity.Post newPost = new com.cc91.entity.Post("New Title", "New Content", me.getId());
            newPost.setCreatedAt(LocalDateTime.now().minusDays(1));
            postRepository.saveAndFlush(newPost);

            com.cc91.entity.Post otherPost = new com.cc91.entity.Post("Other Title", "Other Content", other.getId());
            otherPost.setCreatedAt(LocalDateTime.now());
            postRepository.saveAndFlush(otherPost);

            // Act & Assert
            mockMvc.perform(get("/api/users/me/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("New Title"))
                .andExpect(jsonPath("$[0].authorUsername").value("testuser"))
                .andExpect(jsonPath("$[1].title").value("Old Title"))
                .andExpect(jsonPath("$[1].authorUsername").value("testuser"));
            }

            // ==================== GET /api/users/me/comments 测试 ====================

            @Test
            @WithMockUser(username = "testuser")
            @Transactional
            void getMyComments_Authenticated_ReturnsMyCommentsWithPostTitle() throws Exception {
            // Arrange
            com.cc91.entity.User me = new com.cc91.entity.User(
                "testuser",
                "test@example.com",
                passwordEncoder.encode("password123")
            );
            userRepository.saveAndFlush(me);

            com.cc91.entity.User other = new com.cc91.entity.User(
                "other",
                "other@example.com",
                passwordEncoder.encode("password123")
            );
            userRepository.saveAndFlush(other);

            com.cc91.entity.Post post = new com.cc91.entity.Post("Post Title", "Post Content", other.getId());
            post.setCreatedAt(LocalDateTime.now().minusDays(3));
            postRepository.saveAndFlush(post);

            com.cc91.entity.Comment older = new com.cc91.entity.Comment(post.getId(), me.getId(), "Older Comment", null);
            older.setCreatedAt(LocalDateTime.now().minusDays(2));
            commentRepository.saveAndFlush(older);

            com.cc91.entity.Comment newer = new com.cc91.entity.Comment(post.getId(), me.getId(), "Newer Comment", null);
            newer.setCreatedAt(LocalDateTime.now().minusDays(1));
            commentRepository.saveAndFlush(newer);

            com.cc91.entity.Comment otherUsersComment = new com.cc91.entity.Comment(post.getId(), other.getId(), "Other Comment", null);
            otherUsersComment.setCreatedAt(LocalDateTime.now());
            commentRepository.saveAndFlush(otherUsersComment);

            // Act & Assert
            mockMvc.perform(get("/api/users/me/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("Newer Comment"))
                .andExpect(jsonPath("$[0].postId").value(post.getId()))
                .andExpect(jsonPath("$[0].postTitle").value("Post Title"))
                .andExpect(jsonPath("$[1].content").value("Older Comment"))
                .andExpect(jsonPath("$[1].postId").value(post.getId()))
                .andExpect(jsonPath("$[1].postTitle").value("Post Title"));
            }
}
