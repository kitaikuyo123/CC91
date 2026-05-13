package com.cc91.service;

import com.cc91.dto.CommentResponse;
import com.cc91.dto.CreateCommentRequest;
import com.cc91.dto.UserCommentResponse;
import com.cc91.entity.Comment;
import com.cc91.entity.Post;
import com.cc91.entity.User;
import com.cc91.exception.ResourceNotFoundException;
import com.cc91.exception.UnauthorizedException;
import com.cc91.repository.CommentRepository;
import com.cc91.repository.PostRepository;
import com.cc91.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论服务
 */
@Service
public class CommentService {

    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository,
                         PostRepository postRepository,
                         UserRepository userRepository,
                         NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * 创建评论
     */
    @Transactional
    public CommentResponse createComment(String username, Long postId, CreateCommentRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));

        Comment comment = new Comment(postId, user.getId(), request.getContent(), null);
        comment = commentRepository.save(comment);

        // 如果评论的不是自己的帖子，通知帖子作者
        if (!post.getAuthorId().equals(user.getId())) {
            User postAuthor = userRepository.findById(post.getAuthorId()).orElse(null);
            if (postAuthor != null) {
                notificationService.createNotification(
                        post.getAuthorId(),
                        "REPLY",
                        "新评论通知",
                        user.getUsername() + " 评论了你的帖子: " + post.getTitle(),
                        comment.getId()
                );
            }
        }

        logger.info("评论创建成功: id={}, postId={}, author={}", comment.getId(), postId, username);

        return toCommentResponse(comment, user.getUsername());
    }

    /**
     * 回复评论
     */
    @Transactional
    public CommentResponse replyToComment(String username, Long commentId, CreateCommentRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        Comment parentComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("评论不存在"));

        Comment reply = new Comment(parentComment.getPostId(), user.getId(),
                                    request.getContent(), commentId);
        reply = commentRepository.save(reply);

        // 通知被回复的评论作者（如果不是自己回复自己）
        if (!parentComment.getAuthorId().equals(user.getId())) {
            User parentAuthor = userRepository.findById(parentComment.getAuthorId()).orElse(null);
            if (parentAuthor != null) {
                Post post = postRepository.findById(parentComment.getPostId()).orElse(null);
                String postTitle = post != null ? post.getTitle() : "未知帖子";
                notificationService.createNotification(
                        parentComment.getAuthorId(),
                        "REPLY",
                        "新回复通知",
                        user.getUsername() + " 回复了你在「" + postTitle + "」中的评论",
                        reply.getId()
                );
            }
        }

        logger.info("回复评论成功: id={}, parentId={}, author={}", reply.getId(), commentId, username);

        return toCommentResponse(reply, user.getUsername());
    }

    /**
     * 删除评论（软删除）
     */
    @Transactional
    public void deleteComment(String username, Long commentId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("评论不存在"));

        // 验证作者权限
        if (!comment.getAuthorId().equals(user.getId())) {
            throw new UnauthorizedException("无权限删除此评论");
        }

        // 软删除该评论及其所有子评论
        comment.setStatus("DELETED");
        commentRepository.save(comment);

        // 同时软删除所有子评论
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

        // 查询所有已发布的评论
        List<Comment> comments = commentRepository.findByPostIdAndStatusOrderByCreatedAtAsc(postId, "PUBLISHED");

        // 批量查询所有相关用户
        Set<Long> authorIds = comments.stream()
                .map(Comment::getAuthorId)
                .collect(Collectors.toSet());

        Map<Long, String> userMap = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        // 构建树形结构
        return buildCommentTree(comments, userMap);
    }

        /**
         * 获取当前用户的评论列表（用于 Dashboard "我的评论"）
         */
        @Transactional(readOnly = true)
        public List<UserCommentResponse> getMyComments(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

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
    private List<CommentResponse> buildCommentTree(List<Comment> comments, Map<Long, String> userMap) {
        // 转换为 CommentResponse
        List<CommentResponse> responses = comments.stream()
                .map(comment -> toCommentResponse(comment, userMap.get(comment.getAuthorId())))
                .collect(Collectors.toList());

        // 构建 ID 到 Response 的映射
        Map<Long, CommentResponse> responseMap = new HashMap<>();
        for (CommentResponse response : responses) {
            responseMap.put(response.getId(), response);
        }

        // 构建树形结构
        List<CommentResponse> rootComments = new ArrayList<>();
        for (CommentResponse response : responses) {
            if (response.getParentId() == null) {
                // 顶级评论
                rootComments.add(response);
            } else {
                // 子评论，添加到父评论的 replies 中
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
    private CommentResponse toCommentResponse(Comment comment, String authorUsername) {
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthorId(),
                authorUsername,
                comment.getContent(),
                comment.getParentId(),
                comment.getCreatedAt(),
                comment.getStatus()
        );
    }
}
