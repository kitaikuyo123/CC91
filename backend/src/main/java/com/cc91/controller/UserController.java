package com.cc91.controller;

import com.cc91.dto.ApiResponse;
import com.cc91.dto.ChangePasswordRequest;
import com.cc91.dto.PostResponse;
import com.cc91.dto.UpdateUserProfileRequest;
import com.cc91.dto.UserCommentResponse;
import com.cc91.dto.UserProfileDTO;
import com.cc91.exception.UnauthorizedException;
import com.cc91.service.CommentService;
import com.cc91.service.PostService;
import com.cc91.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 * 处理用户资料相关的请求
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;

    public UserController(UserService userService, PostService postService, CommentService commentService) {
        this.userService = userService;
        this.postService = postService;
        this.commentService = commentService;
    }

    /**
     * 获取当前用户资料
     * GET /api/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getMyProfile() {
        String username = getCurrentUsername();
        UserProfileDTO profile = userService.getMyProfile(username);
        return ResponseEntity.ok(profile);
    }

    /**
     * 查看指定用户的资料
     * GET /api/users/{username}
     */
    @GetMapping("/{username}")
    public ResponseEntity<UserProfileDTO> getUserProfile(@PathVariable String username) {
        UserProfileDTO profile = userService.getUserProfile(username);
        return ResponseEntity.ok(profile);
    }

    /**
     * 更新当前用户资料
     * PUT /api/users/me/profile
     */
    @PutMapping("/me/profile")
    public ResponseEntity<UserProfileDTO> updateProfile(
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        String username = getCurrentUsername();
        UserProfileDTO profile = userService.updateProfile(username, request);
        return ResponseEntity.ok(profile);
    }

    /**
     * 获取当前用户帖子列表（仅已发布）
     * GET /api/users/me/posts
     */
    @GetMapping("/me/posts")
    public ResponseEntity<List<PostResponse>> getMyPosts() {
        String username = getCurrentUsername();
        return ResponseEntity.ok(postService.getMyPosts(username));
    }

    /**
     * 获取当前用户草稿列表
     * GET /api/users/me/drafts
     */
    @GetMapping("/me/drafts")
    public ResponseEntity<List<PostResponse>> getMyDrafts() {
        String username = getCurrentUsername();
        return ResponseEntity.ok(postService.getMyDrafts(username));
    }

    /**
     * 获取当前用户评论列表
     * GET /api/users/me/comments
     */
    @GetMapping("/me/comments")
    public ResponseEntity<List<UserCommentResponse>> getMyComments() {
        String username = getCurrentUsername();
        return ResponseEntity.ok(commentService.getMyComments(username));
    }

    /**
     * 修改当前用户密码
     * PUT /api/users/me/password
     */
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        String username = getCurrentUsername();
        userService.changePassword(username, request);
        return ResponseEntity.ok(ApiResponse.success("密码修改成功"));
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
