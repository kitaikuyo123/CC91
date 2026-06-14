package com.cc91.forumservice.service;

import com.cc91.forumservice.client.CreateNotificationRequest;
import com.cc91.forumservice.client.NotificationServiceClient;
import com.cc91.forumservice.client.UserInfoDTO;
import com.cc91.forumservice.client.UserServiceClient;
import com.cc91.forumservice.dto.CommentResponse;
import com.cc91.forumservice.dto.CreateCommentRequest;
import com.cc91.forumservice.dto.UserCommentResponse;
import com.cc91.forumservice.entity.Comment;
import com.cc91.forumservice.entity.Post;
import com.cc91.forumservice.exception.ResourceNotFoundException;
import com.cc91.forumservice.util.HtmlSanitizer;
import com.cc91.forumservice.exception.UnauthorizedException;
import com.cc91.forumservice.repository.CommentRepository;
import com.cc91.forumservice.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 评论服务
 */
@Service
public class CommentService {

    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserServiceClient userServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    public CommentService(CommentRepository commentRepository,
                         PostRepository postRepository,
                         UserServiceClient userServiceClient,
                         NotificationServiceClient notificationServiceClient) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userServiceClient = userServiceClient;
        this.notificationServiceClient = notificationServiceClient;
    }

    /**
     * 创建评论
     */
    @Transactional
    public CommentResponse createComment(String username, Long postId, CreateCommentRequest request) {
        UserInfoDTO user = userServiceClient.getUserByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));

        Comment comment = new Comment(postId, user.getId(), HtmlSanitizer.sanitizeContent(request.getContent()), null);
        comment = commentRepository.save(comment);

        // 如果评论的不是自己的帖子，通知帖子作者
        if (!post.getAuthorId().equals(user.getId())) {
            try {
                UserInfoDTO postAuthor = userServiceClient.getUserById(post.getAuthorId());
                if (postAuthor != null) {
                    notificationServiceClient.createNotification(
                            new CreateNotificationRequest(
                                    post.getAuthorId(),
                                    "REPLY",
                                    "新评论通知",
                                    user.getUsername() + " 评论了你的帖子: " + post.getTitle(),
                                    post.getId()
                            )
                    );
                }
            } catch (Exception e) {
                logger.warn("Failed to send notification for comment on post {}", postId, e);
            }
        }

        logger.info("评论创建成功: id={}, postId={}, author={}", comment.getId(), postId, username);

        return toCommentResponse(comment, user.getUsername(), user.getAvatarUrl());
    }

    /**
     * 回复评论
     */
    @Transactional
    public CommentResponse replyToComment(String username, Long commentId, CreateCommentRequest request) {
        UserInfoDTO user = userServiceClient.getUserByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        Comment parentComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("评论不存在"));

        Comment reply = new Comment(parentComment.getPostId(), user.getId(),
                                    HtmlSanitizer.sanitizeContent(request.getContent()), commentId);
        reply = commentRepository.save(reply);

        // 通知被回复的评论作者（如果不是自己回复自己）
        if (!parentComment.getAuthorId().equals(user.getId())) {
            try {
                Post post = postRepository.findById(parentComment.getPostId()).orElse(null);
                String postTitle = post != null ? post.getTitle() : "未知帖子";
                notificationServiceClient.createNotification(
                        new CreateNotificationRequest(
                                parentComment.getAuthorId(),
                                "REPLY",
                                "新回复通知",
                                user.getUsername() + " 回复了你在「" + postTitle + "」中的评论",
                                parentComment.getPostId()
                        )
                );
            } catch (Exception e) {
                logger.warn("Failed to send notification for reply to comment {}", commentId, e);
            }
        }

        logger.info("回复评论成功: id={}, parentId={}, author={}", reply.getId(), commentId, username);

        return toCommentResponse(reply, user.getUsername(), user.getAvatarUrl());
    }

    /**
     * 编辑评论
     */
    @Transactional
    public CommentResponse updateComment(String username, Long commentId, String content) {
        UserInfoDTO user = userServiceClient.getUserByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("评论不存在"));

        if (!comment.getAuthorId().equals(user.getId())) {
            throw new UnauthorizedException("无权限编辑此评论");
        }

        comment.setContent(HtmlSanitizer.sanitizeContent(content));
        comment = commentRepository.save(comment);

        logger.info("评论更新成功: id={}, author={}", commentId, username);

        return toCommentResponse(comment, user.getUsername(), user.getAvatarUrl());
    }

    /**
     * 删除评论（软删除）
     */
    @Transactional
    public void deleteComment(String username, Long commentId) {
        UserInfoDTO user = userServiceClient.getUserByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("评论不存在"));

        if (!comment.getAuthorId().equals(user.getId())) {
            throw new UnauthorizedException("无权限删除此评论");
        }

        comment.setStatus("DELETED");
        commentRepository.save(comment);

        List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(commentId);
        for (Comment reply : replies) {
            reply.setStatus("DELETED");
        }
        commentRepository.saveAll(replies);

        logger.info("评论删除成功: id={}, 关联子评论数={}, author={}", commentId, replies.size(), username);
    }

    /**
     * 获取帖子的所有评论（树形结构）
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPostId(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));

        List<Comment> comments = commentRepository.findByPostIdAndStatusOrderByCreatedAtAsc(postId, "PUBLISHED");

        Set<Long> authorIds = comments.stream()
                .map(Comment::getAuthorId)
                .collect(Collectors.toSet());

        Map<Long, UserInfoDTO> userMap = batchResolveUsers(authorIds);

        return buildCommentTree(comments, userMap);
    }

    /**
     * 获取当前用户的评论列表（用于 Dashboard "我的评论"）
     */
    @Transactional(readOnly = true)
    public List<UserCommentResponse> getMyComments(String username) {
        UserInfoDTO user = userServiceClient.getUserByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        List<Comment> comments = commentRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(user.getId(), "PUBLISHED");
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> postIds = comments.stream()
                .map(Comment::getPostId)
                .collect(Collectors.toSet());

        Map<Long, String> postTitleMap = postRepository.findAllById(postIds).stream()
                .collect(Collectors.toMap(Post::getId, Post::getTitle));

        return comments.stream()
                .map(c -> new UserCommentResponse(
                        c.getId(),
                        c.getPostId(),
                        postTitleMap.getOrDefault(c.getPostId(), "未知帖子"),
                        c.getContent(),
                        c.getParentId(),
                        c.getCreatedAt(),
                        c.getStatus()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 构建评论树形结构
     */
    private List<CommentResponse> buildCommentTree(List<Comment> comments, Map<Long, UserInfoDTO> userMap) {
        List<CommentResponse> responses = comments.stream()
                .map(comment -> {
                    UserInfoDTO user = userMap.get(comment.getAuthorId());
                    String username = user != null ? user.getUsername() : "未知用户";
                    String avatarUrl = user != null ? user.getAvatarUrl() : null;
                    CommentResponse response = toCommentResponse(comment, username, avatarUrl);
                    response.setAuthorAvatarUrl(normalizeAvatarUrl(avatarUrl));
                    return response;
                })
                .collect(Collectors.toList());

        Map<Long, CommentResponse> responseMap = new HashMap<>();
        for (CommentResponse response : responses) {
            responseMap.put(response.getId(), response);
        }

        List<CommentResponse> rootComments = new ArrayList<>();
        for (CommentResponse response : responses) {
            if (response.getParentId() == null) {
                rootComments.add(response);
            } else {
                CommentResponse parent = responseMap.get(response.getParentId());
                if (parent != null) {
                    parent.getReplies().add(response);
                }
            }
        }

        return rootComments;
    }

    /**
     * 转换为 CommentResponse
     */
    private CommentResponse toCommentResponse(Comment comment, String authorUsername, String avatarUrl) {
        CommentResponse response = new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthorId(),
                authorUsername,
                comment.getContent(),
                comment.getParentId(),
                comment.getCreatedAt(),
                comment.getStatus()
        );
        response.setAuthorAvatarUrl(normalizeAvatarUrl(avatarUrl));
        return response;
    }

    private static String normalizeAvatarUrl(String avatarUrl) {
        return avatarUrl != null && !avatarUrl.isEmpty() ? avatarUrl : null;
    }

    private Map<Long, UserInfoDTO> batchResolveUsers(Set<Long> authorIds) {
        if (authorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> idList = new ArrayList<>(authorIds);
        List<UserInfoDTO> users = userServiceClient.getUsersByIds(idList);
        return users.stream()
                .filter(u -> u.getId() != null)
                .collect(Collectors.toMap(UserInfoDTO::getId, u -> u));
    }
}
