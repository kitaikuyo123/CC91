package com.cc91.forumservice.controller;

import com.cc91.forumservice.client.CreateNotificationRequest;
import com.cc91.forumservice.client.NotificationServiceClient;
import com.cc91.forumservice.client.UserInfoDTO;
import com.cc91.forumservice.client.UserServiceClient;
import com.cc91.forumservice.dto.ApiResponse;
import com.cc91.forumservice.dto.PostResponse;
import com.cc91.forumservice.dto.UpdatePostStatusRequest;
import com.cc91.forumservice.entity.Comment;
import com.cc91.forumservice.entity.Post;
import com.cc91.forumservice.exception.ResourceNotFoundException;
import com.cc91.forumservice.repository.CommentRepository;
import com.cc91.forumservice.repository.PostRepository;
import com.cc91.forumservice.service.PostService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContentController {

    private static final Logger logger = LoggerFactory.getLogger(AdminContentController.class);

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostService postService;
    private final NotificationServiceClient notificationServiceClient;
    private final UserServiceClient userServiceClient;

    public AdminContentController(PostRepository postRepository,
                                  CommentRepository commentRepository,
                                  PostService postService,
                                  NotificationServiceClient notificationServiceClient,
                                  UserServiceClient userServiceClient) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postService = postService;
        this.notificationServiceClient = notificationServiceClient;
        this.userServiceClient = userServiceClient;
    }

    @GetMapping("/posts")
    public ResponseEntity<List<PostResponse>> getPostsByStatus(
            @RequestParam(required = false) String status
    ) {
        List<PostResponse> response = postService.getPostList(0, 1000, status).getContent();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/comments")
    public ResponseEntity<List<Map<String, Object>>> getComments() {
        List<Comment> comments = commentRepository.findAllWithPostByOrderByCreatedAtDesc();

        Set<Long> authorIds = comments.stream()
                .map(Comment::getAuthorId)
                .collect(Collectors.toSet());

        Map<Long, UserInfoDTO> userMap;
        try {
            List<UserInfoDTO> users = userServiceClient.getUsersByIds(List.copyOf(authorIds));
            userMap = users.stream().collect(Collectors.toMap(UserInfoDTO::getId, u -> u));
        } catch (Exception e) {
            logger.warn("批量查询用户信息失败: {}", e.getMessage());
            userMap = Map.of();
        }
        final Map<Long, UserInfoDTO> finalUserMap = userMap;

        List<Map<String, Object>> result = comments.stream().map(c -> {
            UserInfoDTO author = finalUserMap.get(c.getAuthorId());
            String postTitle = c.getPost() != null ? c.getPost().getTitle() : "未知帖子";
            return Map.<String, Object>of(
                    "id", c.getId(),
                    "postId", c.getPostId(),
                    "postTitle", postTitle,
                    "authorId", c.getAuthorId(),
                    "authorUsername", author != null ? author.getUsername() : "未知用户",
                    "content", c.getContent(),
                    "parentId", c.getParentId() != null ? c.getParentId() : 0L,
                    "status", c.getStatus(),
                    "createdAt", c.getCreatedAt().toString()
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PutMapping("/posts/{id}/status")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> updatePostStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostStatusRequest request
    ) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));

        post.setStatus(request.getStatus());
        postRepository.save(post);

        try {
            notificationServiceClient.createNotification(new CreateNotificationRequest(
                    post.getAuthorId(), "POST_STATUS", "帖子状态变更",
                    "您的帖子「" + post.getTitle() + "」状态已变更为：" + request.getStatus(),
                    post.getId()
            ));
        } catch (Exception e) {
            logger.warn("通知发送失败: postId={}", id, e);
        }

        logger.info("管理员修改帖子状态: id={}, status={}", id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("帖子状态已更新"));
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Void>> forceDeletePost(@PathVariable Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        postRepository.delete(post);
        logger.info("管理员强制删除帖子: id={}", id);
        return ResponseEntity.ok(ApiResponse.success("帖子已删除"));
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<ApiResponse<Void>> forceDeleteComment(@PathVariable Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("评论不存在"));
        commentRepository.delete(comment);
        logger.info("管理员强制删除评论: id={}", id);
        return ResponseEntity.ok(ApiResponse.success("评论已删除"));
    }
}
