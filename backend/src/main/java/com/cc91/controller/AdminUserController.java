package com.cc91.controller;

import com.cc91.dto.AdminUserDTO;
import com.cc91.dto.ApiResponse;
import com.cc91.entity.User;
import com.cc91.exception.ResourceNotFoundException;
import com.cc91.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员 - 用户管理控制器
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserController.class);

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 获取所有用户列表
     * GET /api/admin/users
     */
    @GetMapping
    public ResponseEntity<List<AdminUserDTO>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<AdminUserDTO> userDTOs = users.stream()
                .map(this::toAdminUserDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDTOs);
    }

    /**
     * 封禁用户
     * PUT /api/admin/users/{id}/ban
     */
    @PutMapping("/{id}/ban")
    public ResponseEntity<ApiResponse<Void>> banUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        user.setIsLocked(true);
        user.setLockUntil(null); // 永久封禁
        userRepository.save(user);

        logger.info("管理员封禁用户: id={}, username={}", id, user.getUsername());

        return ResponseEntity.ok(ApiResponse.success("用户已封禁"));
    }

    /**
     * 解封用户
     * PUT /api/admin/users/{id}/unban
     */
    @PutMapping("/{id}/unban")
    public ResponseEntity<ApiResponse<Void>> unbanUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        user.setIsLocked(false);
        user.setLockUntil(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        logger.info("管理员解封用户: id={}, username={}", id, user.getUsername());

        return ResponseEntity.ok(ApiResponse.success("用户已解封"));
    }

    /**
     * 转换为 AdminUserDTO
     */
    private AdminUserDTO toAdminUserDTO(User user) {
        return new AdminUserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getIsLocked(),
                user.getCreatedAt(),
                user.getLockUntil()
        );
    }
}
