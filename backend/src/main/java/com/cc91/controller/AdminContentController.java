package com.cc91.controller;

import com.cc91.dto.ApiResponse;
import com.cc91.dto.PostResponse;
import com.cc91.dto.UpdatePostStatusRequest;
import com.cc91.entity.Post;
import com.cc91.exception.ResourceNotFoundException;
import com.cc91.repository.CommentRepository;
import com.cc91.repository.PostRepository;
import com.cc91.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员 - 内容审核控制器
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContentController {

    private static final Logger logger = LoggerFactory.getLogger(AdminContentController.class);

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostService postService;

    public AdminContentController(PostRepository postRepository,
                                  CommentRepository commentRepository,
                                  PostService postService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postService = postService;
    }

    /**
     * 查看所有帖子（按状态筛选）
     * GET /api/admin/posts?status=PUBLISHED&page=0&size=20
     */
    @GetMapping("/posts")
    public ResponseEntity<Page<PostResponse>> getPostsByStatus(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<PostResponse> response = postService.getPostList(page, size, status);
        return ResponseEntity.ok(response);
    }

    /**
     * 修改帖子状态
     * PUT /api/admin/posts/{id}/status
     */
    @PutMapping("/posts/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updatePostStatus(
            @PathVariable Long id,
            @RequestBody UpdatePostStatusRequest request
    ) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));

        post.setStatus(request.getStatus());
        postRepository.save(post);

        logger.info("管理员修改帖子状态: id={}, status={}", id, request.getStatus());

        return ResponseEntity.ok(ApiResponse.success("帖子状态已更新"));
    }

    /**
     * 强制删除帖子
     * DELETE /api/admin/posts/{id}
     */
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Void>> forceDeletePost(@PathVariable Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));

        postRepository.delete(post);

        logger.info("管理员强制删除帖子: id={}", id);

        return ResponseEntity.ok(ApiResponse.success("帖子已删除"));
    }

    /**
     * 强制删除评论
     * DELETE /api/admin/comments/{id}
     */
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<ApiResponse<Void>> forceDeleteComment(@PathVariable Long id) {
        com.cc91.entity.Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("评论不存在"));

        commentRepository.delete(comment);

        logger.info("管理员强制删除评论: id={}", id);

        return ResponseEntity.ok(ApiResponse.success("评论已删除"));
    }
}
