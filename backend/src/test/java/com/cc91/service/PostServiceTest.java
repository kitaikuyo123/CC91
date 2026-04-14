package com.cc91.service;

import com.cc91.dto.CreatePostRequest;
import com.cc91.dto.PostResponse;
import com.cc91.dto.UpdatePostRequest;
import com.cc91.entity.Post;
import com.cc91.entity.User;
import com.cc91.exception.ResourceNotFoundException;
import com.cc91.exception.UnauthorizedException;
import com.cc91.repository.PostRepository;
import com.cc91.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostService 业务逻辑测试
 * 测试帖子管理的业务路径
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PostServiceTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== createPost 方法测试 ====================

    @Test
    @Transactional
    void createPost_ValidRequest_CreatesPost() {
        // Arrange: 创建一个用户
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        CreatePostRequest request = new CreatePostRequest("测试标题", "测试内容");

        // Act: 创建帖子
        PostResponse result = postService.createPost("author", request);

        // Assert: 验证返回的帖子
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("测试标题", result.getTitle());
        assertEquals("测试内容", result.getContent());
        assertEquals(user.getId(), result.getAuthorId());
        assertEquals("author", result.getAuthorUsername());
        assertEquals(0, result.getViewCount());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());

        // Assert: 验证数据库中已保存
        Post savedPost = postRepository.findById(result.getId()).orElse(null);
        assertNotNull(savedPost);
        assertEquals("测试标题", savedPost.getTitle());
        assertEquals("PUBLISHED", savedPost.getStatus());
    }

    @Test
    void createPost_UserNotExists_ThrowsResourceNotFoundException() {
        // Arrange: 准备创建请求（用户不存在）
        CreatePostRequest request = new CreatePostRequest("标题", "内容");

        // Act & Assert: 用户不存在抛出异常
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> postService.createPost("nonexistent", request)
        );
        assertEquals("用户不存在", exception.getMessage());
    }

    // ==================== getPostById 方法测试 ====================

    @Test
    @Transactional
    void getPostById_PostExists_ReturnsPostAndIncrementsViewCount() {
        // Arrange: 创建用户和帖子
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post post = new Post("标题", "内容", user.getId());
        post.setViewCount(5);
        postRepository.saveAndFlush(post);
        Long postId = post.getId();
        entityManager.clear();

        // Act: 获取帖子
        PostResponse result = postService.getPostById(postId);

        // Assert: 验证返回的帖子
        assertNotNull(result);
        assertEquals(postId, result.getId());
        assertEquals("标题", result.getTitle());
        assertEquals("内容", result.getContent());
        assertEquals("author", result.getAuthorUsername());
        assertEquals(6, result.getViewCount()); // 浏览次数 +1

        // Assert: 验证数据库中浏览次数已更新
        entityManager.flush();
        entityManager.clear();
        Post updatedPost = postRepository.findById(postId).orElse(null);
        assertNotNull(updatedPost);
        assertEquals(6, updatedPost.getViewCount());
    }

    @Test
    void getPostById_PostNotExists_ThrowsResourceNotFoundException() {
        // Act & Assert: 帖子不存在抛出异常
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> postService.getPostById(999L)
        );
        assertEquals("帖子不存在", exception.getMessage());
    }

    // ==================== updatePost 方法测试 ====================

    @Test
    @Transactional
    void updatePost_ValidRequest_UpdatesPost() {
        // Arrange: 创建用户和帖子
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post post = new Post("旧标题", "旧内容", user.getId());
        postRepository.saveAndFlush(post);
        Long postId = post.getId();
        entityManager.clear();

        UpdatePostRequest request = new UpdatePostRequest("新标题", "新内容");

        // Act: 更新帖子
        PostResponse result = postService.updatePost("author", postId, request);

        // Assert: 验证返回的帖子
        assertEquals(postId, result.getId());
        assertEquals("新标题", result.getTitle());
        assertEquals("新内容", result.getContent());

        // Assert: 验证数据库中已更新
        entityManager.flush();
        entityManager.clear();
        Post updatedPost = postRepository.findById(postId).orElse(null);
        assertNotNull(updatedPost);
        assertEquals("新标题", updatedPost.getTitle());
        assertEquals("新内容", updatedPost.getContent());
    }

    @Test
    void updatePost_PostNotExists_ThrowsResourceNotFoundException() {
        // Arrange: 创建用户
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        UpdatePostRequest request = new UpdatePostRequest("标题", "内容");

        // Act & Assert: 帖子不存在抛出异常
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> postService.updatePost("author", 999L, request)
        );
        assertEquals("帖子不存在", exception.getMessage());
    }

    @Test
    @Transactional
    void updatePost_NotAuthor_ThrowsUnauthorizedException() {
        // Arrange: 创建两个用户
        User author = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(author);

        User otherUser = new User("other", "other@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(otherUser);

        Post post = new Post("标题", "内容", author.getId());
        postRepository.saveAndFlush(post);

        UpdatePostRequest request = new UpdatePostRequest("新标题", "新内容");

        // Act & Assert: 非作者更新抛出异常
        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> postService.updatePost("other", post.getId(), request)
        );
        assertEquals("无权限编辑此帖子", exception.getMessage());
    }

    @Test
    @Transactional
    void updatePost_OnlyTitle_UpdatesTitleOnly() {
        // Arrange: 创建用户和帖子
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post post = new Post("旧标题", "旧内容", user.getId());
        postRepository.saveAndFlush(post);
        Long postId = post.getId();
        entityManager.clear();

        UpdatePostRequest request = new UpdatePostRequest();
        request.setTitle("新标题");
        // content 为 null

        // Act: 只更新标题
        PostResponse result = postService.updatePost("author", postId, request);

        // Assert: 验证只有标题被更新
        assertEquals("新标题", result.getTitle());
        assertEquals("旧内容", result.getContent());

        // Assert: 验证数据库
        entityManager.flush();
        entityManager.clear();
        Post updatedPost = postRepository.findById(postId).orElse(null);
        assertNotNull(updatedPost);
        assertEquals("新标题", updatedPost.getTitle());
        assertEquals("旧内容", updatedPost.getContent());
    }

    @Test
    @Transactional
    void updatePost_OnlyContent_UpdatesContentOnly() {
        // Arrange: 创建用户和帖子
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post post = new Post("旧标题", "旧内容", user.getId());
        postRepository.saveAndFlush(post);
        Long postId = post.getId();
        entityManager.clear();

        UpdatePostRequest request = new UpdatePostRequest();
        request.setContent("新内容");
        // title 为 null

        // Act: 只更新内容
        PostResponse result = postService.updatePost("author", postId, request);

        // Assert: 验证只有内容被更新
        assertEquals("旧标题", result.getTitle());
        assertEquals("新内容", result.getContent());

        // Assert: 验证数据库
        entityManager.flush();
        entityManager.clear();
        Post updatedPost = postRepository.findById(postId).orElse(null);
        assertNotNull(updatedPost);
        assertEquals("旧标题", updatedPost.getTitle());
        assertEquals("新内容", updatedPost.getContent());
    }

    // ==================== deletePost 方法测试 ====================

    @Test
    @Transactional
    void deletePost_ValidRequest_DeletesPost() {
        // Arrange: 创建用户和帖子
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post post = new Post("标题", "内容", user.getId());
        postRepository.saveAndFlush(post);
        Long postId = post.getId();

        // Act: 删除帖子
        postService.deletePost("author", postId);

        // Assert: 验证帖子已删除
        assertFalse(postRepository.findById(postId).isPresent());
    }

    @Test
    void deletePost_PostNotExists_ThrowsResourceNotFoundException() {
        // Arrange: 创建用户
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        // Act & Assert: 帖子不存在抛出异常
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> postService.deletePost("author", 999L)
        );
        assertEquals("帖子不存在", exception.getMessage());
    }

    @Test
    @Transactional
    void deletePost_NotAuthor_ThrowsUnauthorizedException() {
        // Arrange: 创建两个用户
        User author = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(author);

        User otherUser = new User("other", "other@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(otherUser);

        Post post = new Post("标题", "内容", author.getId());
        postRepository.saveAndFlush(post);

        // Act & Assert: 非作者删除抛出异常
        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> postService.deletePost("other", post.getId())
        );
        assertEquals("无权限删除此帖子", exception.getMessage());
    }

    // ==================== getPostList 方法测试 ====================

    @Test
    @Transactional
    void getPostList_ReturnsPublishedPostsInDescendingOrder() {
        // Arrange: 创建用户和多个帖子
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post post1 = new Post("帖子1", "内容1", user.getId());
        postRepository.saveAndFlush(post1);

        Post post2 = new Post("帖子2", "内容2", user.getId());
        postRepository.saveAndFlush(post2);

        Post post3 = new Post("帖子3", "内容3", user.getId());
        postRepository.saveAndFlush(post3);

        // Act: 分页查询
        Page<PostResponse> result = postService.getPostList(0, 10);

        // Assert: 验证返回结果
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        assertEquals(3, result.getContent().size());

        // Assert: 验证按创建时间倒序（最新的在前）
        assertEquals("帖子3", result.getContent().get(0).getTitle());
        assertEquals("帖子2", result.getContent().get(1).getTitle());
        assertEquals("帖子1", result.getContent().get(2).getTitle());
    }

    @Test
    @Transactional
    void getPostList_OnlyReturnsPublishedPosts() {
        // Arrange: 创建用户和不同状态的帖子
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post publishedPost = new Post("已发布", "内容", user.getId());
        publishedPost.setStatus("PUBLISHED");
        postRepository.saveAndFlush(publishedPost);

        Post draftPost = new Post("草稿", "内容", user.getId());
        draftPost.setStatus("DRAFT");
        postRepository.saveAndFlush(draftPost);

        // Act: 查询帖子列表
        Page<PostResponse> result = postService.getPostList(0, 10);

        // Assert: 只返回 PUBLISHED 状态的帖子
        assertEquals(1, result.getTotalElements());
        assertEquals("已发布", result.getContent().get(0).getTitle());
    }

    @Test
    @Transactional
    void getPostList_Pagination_ReturnsCorrectPage() {
        // Arrange: 创建用户和多个帖子
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        for (int i = 1; i <= 15; i++) {
            Post post = new Post("帖子" + i, "内容" + i, user.getId());
            postRepository.saveAndFlush(post);
        }

        // Act: 查询第一页（每页5条）
        Page<PostResponse> page1 = postService.getPostList(0, 5);

        // Assert: 验证第一页
        assertEquals(15, page1.getTotalElements());
        assertEquals(3, page1.getTotalPages());
        assertEquals(5, page1.getContent().size());

        // Act: 查询第二页
        Page<PostResponse> page2 = postService.getPostList(1, 5);

        // Assert: 验证第二页
        assertEquals(5, page2.getContent().size());
        assertNotEquals(page1.getContent().get(0).getId(), page2.getContent().get(0).getId());
    }
}
