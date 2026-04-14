package com.cc91.controller;

import com.cc91.dto.ApiResponse;
import com.cc91.dto.CreatePostRequest;
import com.cc91.dto.PostResponse;
import com.cc91.dto.UpdatePostRequest;
import com.cc91.exception.UnauthorizedException;
import com.cc91.service.PostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 帖子控制器
 * 处理帖子相关的请求
 */
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * 创建帖子
     * POST /api/posts
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @Valid @RequestBody CreatePostRequest request
    ) {
        String username = getCurrentUsername();
        PostResponse post = postService.createPost(username, request);
        return ResponseEntity.ok(ApiResponse.success("帖子创建成功", post));
    }

    /**
     * 获取帖子详情
     * GET /api/posts/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Long id) {
        PostResponse post = postService.getPostById(id);
        return ResponseEntity.ok(post);
    }

    /**
     * 更新帖子
     * PUT /api/posts/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request
    ) {
        String username = getCurrentUsername();
        PostResponse post = postService.updatePost(username, id, request);
        return ResponseEntity.ok(ApiResponse.success("帖子更新成功", post));
    }

    /**
     * 删除帖子
     * DELETE /api/posts/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id) {
        String username = getCurrentUsername();
        postService.deletePost(username, id);
        return ResponseEntity.ok(ApiResponse.success("帖子删除成功"));
    }

    /**
     * 分页查询帖子列表
     * GET /api/posts?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<Page<PostResponse>> getPostList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PostResponse> posts = postService.getPostList(page, size);
        return ResponseEntity.ok(posts);
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
