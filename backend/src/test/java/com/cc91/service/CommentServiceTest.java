package com.cc91.service;

import com.cc91.dto.CommentResponse;
import com.cc91.dto.CreateCommentRequest;
import com.cc91.entity.Comment;
import com.cc91.entity.Post;
import com.cc91.entity.User;
import com.cc91.exception.ResourceNotFoundException;
import com.cc91.exception.UnauthorizedException;
import com.cc91.repository.CommentRepository;
import com.cc91.repository.PostRepository;
import com.cc91.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommentService 业务逻辑测试
 * 测试评论管理的业务路径
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CommentServiceTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

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
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== createComment 方法测试 ====================

    @Test
    @Transactional
    void createComment_ValidRequest_CreatesComment() {
        // Arrange: 创建用户和帖子
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post post = new Post("标题", "内容", user.getId());
        postRepository.saveAndFlush(post);

        CreateCommentRequest request = new CreateCommentRequest("这是一条评论");

        // Act: 创建评论
        CommentResponse result = commentService.createComment("author", post.getId(), request);

        // Assert: 验证返回的评论
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(post.getId(), result.getPostId());
        assertEquals(user.getId(), result.getAuthorId());
        assertEquals("author", result.getAuthorUsername());
        assertEquals("这是一条评论", result.getContent());
        assertNull(result.getParentId());
        assertEquals("PUBLISHED", result.getStatus());
        assertNotNull(result.getCreatedAt());

        // Assert: 验证数据库中已保存
        Comment savedComment = commentRepository.findById(result.getId()).orElse(null);
        assertNotNull(savedComment);
        assertEquals("这是一条评论", savedComment.getContent());
        assertEquals("PUBLISHED", savedComment.getStatus());
    }

    @Test
    void createComment_UserNotExists_ThrowsResourceNotFoundException() {
        // Arrange: 创建帖子（用户不存在）
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post post = new Post("标题", "内容", user.getId());
        postRepository.saveAndFlush(post);

        CreateCommentRequest request = new CreateCommentRequest("评论内容");

        // Act & Assert: 用户不存在抛出异常
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> commentService.createComment("nonexistent", post.getId(), request)
        );
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    void createComment_PostNotExists_ThrowsResourceNotFoundException() {
        // Arrange: 创建用户（帖子不存在）
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        CreateCommentRequest request = new CreateCommentRequest("评论内容");

        // Act & Assert: 帖子不存在抛出异常
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> commentService.createComment("author", 999L, request)
        );
        assertEquals("帖子不存在", exception.getMessage());
    }

    // ==================== replyToComment 方法测试 ====================

    @Test
    @Transactional
    void replyToComment_ValidRequest_CreatesReply() {
        // Arrange: 创建用户、帖子和父评论
        User user1 = new User("user1", "user1@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user1);

        User user2 = new User("user2", "user2@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user2);

        Post post = new Post("标题", "内容", user1.getId());
        postRepository.saveAndFlush(post);

        Comment parentComment = new Comment(post.getId(), user1.getId(), "父评论", null);
        commentRepository.saveAndFlush(parentComment);

        CreateCommentRequest request = new CreateCommentRequest("这是一条回复");

        // Act: 回复评论
        CommentResponse result = commentService.replyToComment("user2", parentComment.getId(), request);

        // Assert: 验证返回的回复
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(post.getId(), result.getPostId());
        assertEquals(user2.getId(), result.getAuthorId());
        assertEquals("user2", result.getAuthorUsername());
        assertEquals("这是一条回复", result.getContent());
        assertEquals(parentComment.getId(), result.getParentId());
        assertEquals("PUBLISHED", result.getStatus());

        // Assert: 验证数据库中已保存
        Comment savedReply = commentRepository.findById(result.getId()).orElse(null);
        assertNotNull(savedReply);
        assertEquals(parentComment.getId(), savedReply.getParentId());
    }

    @Test
    void replyToComment_UserNotExists_ThrowsResourceNotFoundException() {
        // Arrange: 创建帖子和评论
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post post = new Post("标题", "内容", user.getId());
        postRepository.saveAndFlush(post);

        Comment comment = new Comment(post.getId(), user.getId(), "评论", null);
        commentRepository.saveAndFlush(comment);

        CreateCommentRequest request = new CreateCommentRequest("回复内容");

        // Act & Assert: 用户不存在抛出异常
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> commentService.replyToComment("nonexistent", comment.getId(), request)
        );
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    void replyToComment_ParentCommentNotExists_ThrowsResourceNotFoundException() {
        // Arrange: 创建用户
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        CreateCommentRequest request = new CreateCommentRequest("回复内容");

        // Act & Assert: 父评论不存在抛出异常
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> commentService.replyToComment("author", 999L, request)
        );
        assertEquals("评论不存在", exception.getMessage());
    }

    // ==================== deleteComment 方法测试 ====================

    @Test
    @Transactional
    void deleteComment_ValidRequest_SoftDeletesComment() {
        // Arrange: 创建用户、帖子和评论
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post post = new Post("标题", "内容", user.getId());
        postRepository.saveAndFlush(post);

        Comment comment = new Comment(post.getId(), user.getId(), "评论内容", null);
        commentRepository.saveAndFlush(comment);
        Long commentId = comment.getId();
        entityManager.clear();

        // Act: 删除评论
        commentService.deleteComment("author", commentId);

        // Assert: 验证评论状态为 DELETED（软删除）
        entityManager.flush();
        entityManager.clear();
        Comment deletedComment = commentRepository.findById(commentId).orElse(null);
        assertNotNull(deletedComment);
        assertEquals("DELETED", deletedComment.getStatus());
    }

    @Test
    void deleteComment_CommentNotExists_ThrowsResourceNotFoundException() {
        // Arrange: 创建用户
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        // Act & Assert: 评论不存在抛出异常
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> commentService.deleteComment("author", 999L)
        );
        assertEquals("评论不存在", exception.getMessage());
    }

    @Test
    @Transactional
    void deleteComment_NotAuthor_ThrowsUnauthorizedException() {
        // Arrange: 创建两个用户
        User author = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(author);

        User otherUser = new User("other", "other@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(otherUser);

        Post post = new Post("标题", "内容", author.getId());
        postRepository.saveAndFlush(post);

        Comment comment = new Comment(post.getId(), author.getId(), "评论内容", null);
        commentRepository.saveAndFlush(comment);

        // Act & Assert: 非作者删除抛出异常
        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> commentService.deleteComment("other", comment.getId())
        );
        assertEquals("无权限删除此评论", exception.getMessage());
    }

    // ==================== getCommentsByPostId 方法测试 ====================

    @Test
    @Transactional
    void getCommentsByPostId_ReturnsCommentsInTreeStructure() {
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

        Comment reply2 = new Comment(post.getId(), user1.getId(), "回复1-2", comment1.getId());
        commentRepository.saveAndFlush(reply2);

        entityManager.clear();

        // Act: 获取评论列表
        List<CommentResponse> result = commentService.getCommentsByPostId(post.getId());

        // Assert: 验证树形结构
        assertNotNull(result);
        assertEquals(2, result.size()); // 两个顶级评论

        // 验证第一个顶级评论
        CommentResponse firstComment = result.get(0);
        assertEquals("评论1", firstComment.getContent());
        assertEquals(2, firstComment.getReplies().size()); // 两个回复

        // 验证回复
        assertEquals("回复1-1", firstComment.getReplies().get(0).getContent());
        assertEquals("回复1-2", firstComment.getReplies().get(1).getContent());

        // 验证第二个顶级评论
        CommentResponse secondComment = result.get(1);
        assertEquals("评论2", secondComment.getContent());
        assertEquals(0, secondComment.getReplies().size()); // 无回复
    }

    @Test
    void getCommentsByPostId_PostNotExists_ThrowsResourceNotFoundException() {
        // Act & Assert: 帖子不存在抛出异常
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> commentService.getCommentsByPostId(999L)
        );
        assertEquals("帖子不存在", exception.getMessage());
    }

    @Test
    @Transactional
    void getCommentsByPostId_OnlyReturnsPublishedComments() {
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

        entityManager.clear();

        // Act: 获取评论列表
        List<CommentResponse> result = commentService.getCommentsByPostId(post.getId());

        // Assert: 只返回 PUBLISHED 状态的评论
        assertEquals(1, result.size());
        assertEquals("已发布评论", result.get(0).getContent());
    }

    @Test
    @Transactional
    void getCommentsByPostId_NestedReplies_BuildsCorrectTree() {
        // Arrange: 创建用户、帖子和多层嵌套评论
        User user = new User("author", "author@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        Post post = new Post("标题", "内容", user.getId());
        postRepository.saveAndFlush(post);

        // 创建顶级评论
        Comment topComment = new Comment(post.getId(), user.getId(), "顶级评论", null);
        commentRepository.saveAndFlush(topComment);

        // 创建一级回复
        Comment level1Reply = new Comment(post.getId(), user.getId(), "一级回复", topComment.getId());
        commentRepository.saveAndFlush(level1Reply);

        // 创建二级回复
        Comment level2Reply = new Comment(post.getId(), user.getId(), "二级回复", level1Reply.getId());
        commentRepository.saveAndFlush(level2Reply);

        entityManager.clear();

        // Act: 获取评论列表
        List<CommentResponse> result = commentService.getCommentsByPostId(post.getId());

        // Assert: 验证嵌套结构
        assertEquals(1, result.size());
        CommentResponse top = result.get(0);
        assertEquals("顶级评论", top.getContent());
        assertEquals(1, top.getReplies().size());

        CommentResponse level1 = top.getReplies().get(0);
        assertEquals("一级回复", level1.getContent());
        assertEquals(1, level1.getReplies().size());

        CommentResponse level2 = level1.getReplies().get(0);
        assertEquals("二级回复", level2.getContent());
        assertEquals(0, level2.getReplies().size());
    }
}
