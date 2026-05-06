package com.cc91.controller;

import com.cc91.entity.User;
import com.cc91.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminUserController HTTP endpoint tests
 * Tests admin-only user management endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ==================== GET /api/admin/users ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Transactional
    void getAllUsers_AsAdmin_Returns200() throws Exception {
        // Arrange
        User user1 = new User("user1", "user1@example.com", passwordEncoder.encode("pass"));
        User user2 = new User("user2", "user2@example.com", passwordEncoder.encode("pass"));
        userRepository.save(user1);
        userRepository.save(user2);

        // Act & Assert
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @WithMockUser(username = "user")
    void getAllUsers_AsRegularUser_Returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/admin/users/{id}/ban ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Transactional
    void banUser_AsAdmin_Returns200() throws Exception {
        // Arrange
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("pass"));
        user = userRepository.save(user);

        // Act & Assert
        mockMvc.perform(put("/api/admin/users/" + user.getId() + "/ban"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("用户已封禁"));

        // Verify user is locked
        User lockedUser = userRepository.findById(user.getId()).orElse(null);
        assert lockedUser != null;
        assert lockedUser.getIsLocked();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void banUser_UserNotExists_Returns404() throws Exception {
        mockMvc.perform(put("/api/admin/users/999/ban"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    @Test
    @WithMockUser(username = "user")
    @Transactional
    void banUser_AsRegularUser_Returns403() throws Exception {
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("pass"));
        user = userRepository.save(user);

        mockMvc.perform(put("/api/admin/users/" + user.getId() + "/ban"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/admin/users/{id}/unban ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Transactional
    void unbanUser_AsAdmin_Returns200() throws Exception {
        // Arrange
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("pass"));
        user.setIsLocked(true);
        user.setFailedLoginAttempts(5);
        user = userRepository.save(user);

        // Act & Assert
        mockMvc.perform(put("/api/admin/users/" + user.getId() + "/unban"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("用户已解封"));

        // Verify user is unlocked
        User unlockedUser = userRepository.findById(user.getId()).orElse(null);
        assert unlockedUser != null;
        assert !unlockedUser.getIsLocked();
        assert unlockedUser.getFailedLoginAttempts() == 0;
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void unbanUser_UserNotExists_Returns404() throws Exception {
        mockMvc.perform(put("/api/admin/users/999/unban"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    @Test
    @WithMockUser(username = "user")
    @Transactional
    void unbanUser_AsRegularUser_Returns403() throws Exception {
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("pass"));
        user = userRepository.save(user);

        mockMvc.perform(put("/api/admin/users/" + user.getId() + "/unban"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Transactional
    void unbanUser_AlreadyUnlocked_Returns200() throws Exception {
        // Arrange: user is already unlocked
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("pass"));
        user = userRepository.save(user);

        // Act & Assert: should still succeed
        mockMvc.perform(put("/api/admin/users/" + user.getId() + "/unban"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("用户已解封"));
    }
}
