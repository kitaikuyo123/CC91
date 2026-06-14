package com.cc91.userservice.controller;

import com.cc91.userservice.dto.ApiResponse;
import com.cc91.userservice.dto.ChangePasswordRequest;
import com.cc91.userservice.dto.UpdateUserProfileRequest;
import com.cc91.userservice.dto.UserProfileDTO;
import com.cc91.userservice.exception.UnauthorizedException;
import com.cc91.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * 处理用户资料相关的请求
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
