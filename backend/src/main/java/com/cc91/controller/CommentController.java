package com.cc91.controller;

import com.cc91.dto.ApiResponse;
import com.cc91.dto.CommentResponse;
import com.cc91.dto.CreateCommentRequest;
import com.cc91.exception.UnauthorizedException;
import com.cc91.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论控制器
 * 处理评论相关的请求
 */
@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 创建评论
     * POST /api/posts/{postId}/comments
     */
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        String username = getCurrentUsername();
        CommentResponse comment = commentService.createComment(username, postId, request);
        return ResponseEntity.ok(ApiResponse.success("评论创建成功", comment));
    }

    /**
     * 回复评论
     * POST /api/comments/{id}/reply
     */
    @PostMapping("/comments/{id}/reply")
    public ResponseEntity<ApiResponse<CommentResponse>> replyToComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        String username = getCurrentUsername();
        CommentResponse comment = commentService.replyToComment(username, id, request);
        return ResponseEntity.ok(ApiResponse.success("回复成功", comment));
    }

    /**
     * 删除评论
     * DELETE /api/comments/{id}
     */
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long id) {
        String username = getCurrentUsername();
        commentService.deleteComment(username, id);
        return ResponseEntity.ok(ApiResponse.success("评论删除成功"));
    }

    /**
     * 获取帖子的所有评论
     * GET /api/posts/{postId}/comments
     */
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentResponse>> getCommentsByPostId(@PathVariable Long postId) {
        List<CommentResponse> comments = commentService.getCommentsByPostId(postId);
        return ResponseEntity.ok(comments);
    }

    /**
     * 从 Spring Security 上下文中获取当前登录用户名
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        throw new UnauthorizedException("用户未登录");
    }
}
