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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public CommentService(CommentRepository commentRepository,
                         PostRepository postRepository,
                         UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
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

        // 软删除
        comment.setStatus("DELETED");
        commentRepository.save(comment);

        logger.info("评论删除成功: id={}, author={}", commentId, username);
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

        // 构建用户名映射
        Map<Long, String> userMap = new HashMap<>();
        for (Comment comment : comments) {
            if (!userMap.containsKey(comment.getAuthorId())) {
                User author = userRepository.findById(comment.getAuthorId()).orElse(null);
                userMap.put(comment.getAuthorId(), author != null ? author.getUsername() : "未知用户");
            }
        }

        // 构建树形结构
        return buildCommentTree(comments, userMap);
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
