package com.cc91.userservice.controller;

import com.cc91.userservice.dto.UserInfoDTO;
import com.cc91.userservice.entity.User;
import com.cc91.userservice.repository.UserRepository;
import com.cc91.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Internal API for inter-service user lookups.
 * These endpoints are not exposed to the public — they are used by other microservices.
 */
@RestController
@RequestMapping("/api/users/internal")
public class InternalUserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public InternalUserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserInfoDTO> getUserById(@PathVariable Long id) {
        UserInfoDTO userInfo = userService.getUserInfoById(id);
        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/{userId}/locked")
    public ResponseEntity<Map<String, Boolean>> isUserLocked(@PathVariable Long userId) {
        boolean locked = userRepository.findById(userId)
                .map(User::getIsLocked)
                .orElse(false);
        return ResponseEntity.ok(Map.of("locked", locked));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserInfoDTO> getUserByUsername(@PathVariable String username) {
        UserInfoDTO userInfo = userService.getUserInfoByUsername(username);
        return ResponseEntity.ok(userInfo);
    }

    /**
     * Batch lookup: resolve multiple user IDs in a single call.
     * POST /api/users/internal/batch
     */
    @PostMapping("/batch")
    public ResponseEntity<List<UserInfoDTO>> getUsersByIds(@RequestBody List<Long> ids) {
        List<UserInfoDTO> users = ids.stream()
                .map(id -> {
                    try {
                        return userService.getUserInfoById(id);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(u -> u != null)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * Update user avatar URL (called by File Service after avatar upload).
     * PUT /api/users/internal/{id}/avatar
     */
    @PutMapping("/{id}/avatar")
    public ResponseEntity<Void> updateAvatar(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.updateAvatarInternal(id, body.get("avatarUrl"));
        return ResponseEntity.ok().build();
    }
}
