package com.cc91.controller;

import com.cc91.entity.Category;
import com.cc91.entity.Post;
import com.cc91.entity.User;
import com.cc91.repository.CategoryRepository;
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
 * PostController 业务逻辑测试
 * 测试帖子相关的 HTTP 接口
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PostControllerTest {

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
    private CategoryRepository categoryRepository;

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
        postRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== POST /api/posts 测试 ====================

    @Test
    @WithMockUser(username = "author")
    @Transactional
    void createPost_Authenticated_ReturnsCreatedPost() throws Exception {
        // Arrange: 创建用户和版块
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.save(user);

        Category category = new Category("技术讨论", "技术相关话题", 1);
        categoryRepository.saveAndFlush(category);

        String requestBody = """
            {
                "title": "测试标题",
                "content": "测试内容",
                "categoryId": %d
            }
            """.formatted(category.getId());

        // Act & Assert: 认证用户可以创建帖子
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("帖子创建成功"))
                .andExpect(jsonPath("$.data.title").value("测试标题"))
                .andExpect(jsonPath("$.data.content").value("测试内容"))
                .andExpect(jsonPath("$.data.authorUsername").value("author"))
                .andExpect(jsonPath("$.data.viewCount").value(0));
    }

    @Test
    @WithMockUser(username = "nonexistent")
    void createPost_UserNotExists_Returns404() throws Exception {
        // Arrange: 创建版块（categoryId 必填，否则 @NotNull 校验先触发 400）
        Category category = new Category("技术讨论", "技术相关话题", 1);
        categoryRepository.saveAndFlush(category);

        String requestBody = """
            {
                "title": "标题",
                "content": "内容",
                "categoryId": %d
            }
            """.formatted(category.getId());

        // Act & Assert: 用户不存在返回 404
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    // ==================== GET /api/posts/{id} 测试 ====================

    @Test
    @Transactional
    void getPostById_PostExists_ReturnsPost() throws Exception {
        // Arrange: 创建用户和帖子
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post post = new Post("标题", "内容", user.getId());
        post.setViewCount(5);
        postRepository.saveAndFlush(post);

        // Act & Assert: 获取帖子详情
        mockMvc.perform(get("/api/posts/" + post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(post.getId()))
                .andExpect(jsonPath("$.title").value("标题"))
                .andExpect(jsonPath("$.content").value("内容"))
                .andExpect(jsonPath("$.authorUsername").value("author"))
                .andExpect(jsonPath("$.viewCount").value(6)); // 浏览次数 +1
    }

    @Test
    void getPostById_PostNotExists_Returns404() throws Exception {
        // Act & Assert: 帖子不存在返回 404
        mockMvc.perform(get("/api/posts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("帖子不存在"));
    }

    // ==================== PUT /api/posts/{id} 测试 ====================

    @Test
    @WithMockUser(username = "author")
    @Transactional
    void updatePost_Authenticated_ReturnsUpdatedPost() throws Exception {
        // Arrange: 创建用户和帖子
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post post = new Post("旧标题", "旧内容", user.getId());
        postRepository.saveAndFlush(post);

        String requestBody = """
            {
                "title": "新标题",
                "content": "新内容"
            }
            """;

        // Act & Assert: 作者可以更新帖子
        mockMvc.perform(put("/api/posts/" + post.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("帖子更新成功"))
                .andExpect(jsonPath("$.data.title").value("新标题"))
                .andExpect(jsonPath("$.data.content").value("新内容"));
    }

    @Test
    @WithMockUser(username = "other")
    @Transactional
    void updatePost_NotAuthor_Returns403() throws Exception {
        // Arrange: 创建两个用户
        User author = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(author);

        User otherUser = new User("other", "other@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(otherUser);

        Post post = new Post("标题", "内容", author.getId());
        postRepository.saveAndFlush(post);

        String requestBody = """
            {
                "title": "新标题",
                "content": "新内容"
            }
            """;

        // Act & Assert: 非作者更新返回 403
        mockMvc.perform(put("/api/posts/" + post.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("无权限编辑此帖子"));
    }

    @Test
    @WithMockUser(username = "author")
    void updatePost_PostNotExists_Returns404() throws Exception {
        // Arrange: 创建用户
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        String requestBody = """
            {
                "title": "新标题",
                "content": "新内容"
            }
            """;

        // Act & Assert: 帖子不存在返回 404
        mockMvc.perform(put("/api/posts/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("帖子不存在"));
    }

    // ==================== DELETE /api/posts/{id} 测试 ====================

    @Test
    @WithMockUser(username = "author")
    @Transactional
    void deletePost_Authenticated_ReturnsSuccess() throws Exception {
        // Arrange: 创建用户和帖子
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post post = new Post("标题", "内容", user.getId());
        postRepository.saveAndFlush(post);

        // Act & Assert: 作者可以删除帖子
        mockMvc.perform(delete("/api/posts/" + post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("帖子删除成功"));
    }

    @Test
    @WithMockUser(username = "other")
    @Transactional
    void deletePost_NotAuthor_Returns403() throws Exception {
        // Arrange: 创建两个用户
        User author = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(author);

        User otherUser = new User("other", "other@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(otherUser);

        Post post = new Post("标题", "内容", author.getId());
        postRepository.saveAndFlush(post);

        // Act & Assert: 非作者删除返回 403
        mockMvc.perform(delete("/api/posts/" + post.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("无权限删除此帖子"));
    }

    @Test
    @WithMockUser(username = "author")
    void deletePost_PostNotExists_Returns404() throws Exception {
        // Arrange: 创建用户
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        // Act & Assert: 帖子不存在返回 404
        mockMvc.perform(delete("/api/posts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("帖子不存在"));
    }

    // ==================== GET /api/posts 测试 ====================

    @Test
    @Transactional
    void getPostList_ReturnsPagedPosts() throws Exception {
        // Arrange: 创建用户和多个帖子
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        for (int i = 1; i <= 3; i++) {
            Post post = new Post("帖子" + i, "内容" + i, user.getId());
            postRepository.saveAndFlush(post);
        }

        // Act & Assert: 查询帖子列表
        mockMvc.perform(get("/api/posts")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @Transactional
    void getPostList_DefaultPagination_ReturnsFirstPage() throws Exception {
        // Arrange: 创建用户和帖子
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post post = new Post("标题", "内容", user.getId());
        postRepository.saveAndFlush(post);

        // Act & Assert: 不传分页参数，使用默认值
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    // ==================== GET /api/posts/by-category/{categoryId} 测试 ====================

    @Test
    @WithMockUser(username = "author")
    @Transactional
    void getPostsByCategory_CategoryWithPosts_ReturnsFilteredPosts() throws Exception {
        // Arrange
        User author = new User("author", "author@test.com", passwordEncoder.encode("password123"));
        author.setRole("USER");
        userRepository.saveAndFlush(author);

        Category category = new Category("技术交流", "讨论技术相关话题", 0);
        categoryRepository.saveAndFlush(category);

        Post post = new Post("分类下的帖子", "内容", author.getId());
        post.setCategoryId(category.getId());
        post.setStatus("PUBLISHED");
        postRepository.saveAndFlush(post);

        // Act & Assert
        mockMvc.perform(get("/api/posts/by-category/{categoryId}", category.getId())
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("分类下的帖子"));
    }

    @Test
    @WithMockUser(username = "author")
    void getPostsByCategory_NonExistentCategory_Returns404() throws Exception {
        mockMvc.perform(get("/api/posts/by-category/{categoryId}", 99999L)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isNotFound());
    }

    // ==================== GET /api/posts/search 测试 ====================

    @Test
    @WithMockUser(username = "author")
    @Transactional
    void searchPosts_MatchingKeyword_ReturnsResults() throws Exception {
        // Arrange
        User author = new User("author", "author@test.com", passwordEncoder.encode("password123"));
        author.setRole("USER");
        userRepository.saveAndFlush(author);

        Post post1 = new Post("Spring Boot 教程", "Spring Boot 入门指南", author.getId());
        post1.setStatus("PUBLISHED");
        postRepository.saveAndFlush(post1);

        Post post2 = new Post("React 学习笔记", "React 前端开发", author.getId());
        post2.setStatus("PUBLISHED");
        postRepository.saveAndFlush(post2);

        // Act & Assert - search for "Spring"
        mockMvc.perform(get("/api/posts/search")
                .param("keyword", "Spring")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Spring Boot 教程"));
    }

    @Test
    @WithMockUser(username = "author")
    void searchPosts_NoMatch_ReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/posts/search")
                .param("keyword", "不存在的关键词")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));
    }
}
