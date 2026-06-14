package com.cc91.forumservice.controller;

import com.cc91.forumservice.dto.ApiResponse;
import com.cc91.forumservice.dto.CreatePostRequest;
import com.cc91.forumservice.dto.PostResponse;
import com.cc91.forumservice.dto.UpdatePostRequest;
import com.cc91.forumservice.exception.UnauthorizedException;
import com.cc91.forumservice.service.PostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
     * GET /api/posts/{id}?increaseView=true
     */
    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPostById(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean increaseView
    ) {
        PostResponse post = postService.getPostById(id, increaseView);
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
     * GET /api/posts?page=0&size=10&status=PUBLISHED&sort=latest
     */
    @GetMapping
    public ResponseEntity<Page<PostResponse>> getPostList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "PUBLISHED") String status,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !"PUBLISHED".equals(status)) {
            status = "PUBLISHED";
        }
        Page<PostResponse> posts = postService.getPostList(page, size, status, sort);
        return ResponseEntity.ok(posts);
    }

    /**
     * 根据版块ID分页查询帖子
     * GET /api/posts/by-category/{categoryId}?page=0&size=10
     */
    @GetMapping("/by-category/{categoryId}")
    public ResponseEntity<Page<PostResponse>> getPostsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PostResponse> posts = postService.getPostsByCategory(categoryId, page, size);
        return ResponseEntity.ok(posts);
    }

    /**
     * 搜索帖子（按标题或内容）
     * GET /api/posts/search?keyword=xxx&page=0&size=10
     */
    @GetMapping("/search")
    public ResponseEntity<Page<PostResponse>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PostResponse> posts = postService.searchPosts(keyword, page, size);
        return ResponseEntity.ok(posts);
    }

    /**
     * 切换点赞状态（点赞/取消点赞）
     * POST /api/posts/{id}/like
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleLike(@PathVariable Long id) {
        String username = getCurrentUsername();
        Map<String, Object> result = postService.toggleLike(username, id);
        return ResponseEntity.ok(ApiResponse.success("操作成功", result));
    }

    /**
     * 切换收藏状态（收藏/取消收藏）
     * POST /api/posts/{id}/bookmark
     */
    @PostMapping("/{id}/bookmark")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleBookmark(@PathVariable Long id) {
        String username = getCurrentUsername();
        Map<String, Object> result = postService.toggleBookmark(username, id);
        return ResponseEntity.ok(ApiResponse.success("操作成功", result));
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
