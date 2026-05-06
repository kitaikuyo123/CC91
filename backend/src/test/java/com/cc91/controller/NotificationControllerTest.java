package com.cc91.controller;

import com.cc91.entity.Notification;
import com.cc91.entity.User;
import com.cc91.repository.NotificationRepository;
import com.cc91.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * NotificationController HTTP endpoint tests
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        testUser = userRepository.save(testUser);
    }

    // ==================== GET /api/notifications ====================

    @Test
    @WithMockUser(username = "testuser")
    @Transactional
    void getNotifications_ReturnsNotificationList() throws Exception {
        // Arrange
        Notification notif1 = new Notification(testUser.getId(), "COMMENT", "New comment", "Content", 1L);
        Notification notif2 = new Notification(testUser.getId(), "LIKE", "New like", "Content", 2L);
        notificationRepository.save(notif1);
        notificationRepository.save(notif2);

        // Act & Assert
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getNotifications_Empty_ReturnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "testuser")
    @Transactional
    void getNotifications_WithPagination_ReturnsCorrectPageSize() throws Exception {
        // Arrange: create 5 notifications
        for (int i = 0; i < 5; i++) {
            Notification notif = new Notification(testUser.getId(), "TEST", "Title", "Content", 1L);
            notificationRepository.save(notif);
        }

        // Act & Assert
        mockMvc.perform(get("/api/notifications?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ==================== GET /api/notifications/unread-count ====================

    @Test
    @WithMockUser(username = "testuser")
    @Transactional
    void getUnreadCount_ReturnsCorrectCount() throws Exception {
        // Arrange
        Notification notif1 = new Notification(testUser.getId(), "COMMENT", "Title", "Content", 1L);
        Notification notif2 = new Notification(testUser.getId(), "LIKE", "Title", "Content", 2L);
        notif2.setIsRead(true);
        notificationRepository.save(notif1);
        notificationRepository.save(notif2);

        // Act & Assert
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getUnreadCount_NoNotifications_ReturnsZero() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    // ==================== PUT /api/notifications/{id}/read ====================

    @Test
    @WithMockUser(username = "testuser")
    @Transactional
    void markAsRead_ValidNotification_Returns200() throws Exception {
        // Arrange
        Notification notif = new Notification(testUser.getId(), "COMMENT", "Title", "Content", 1L);
        notif = notificationRepository.save(notif);

        // Act & Assert
        mockMvc.perform(put("/api/notifications/" + notif.getId() + "/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("通知已标记为已读"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void markAsRead_NotificationNotExists_Returns404() throws Exception {
        mockMvc.perform(put("/api/notifications/999/read"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("通知不存在"));
    }

    // ==================== PUT /api/notifications/read-all ====================

    @Test
    @WithMockUser(username = "testuser")
    @Transactional
    void markAllAsRead_Returns200() throws Exception {
        // Arrange
        Notification notif1 = new Notification(testUser.getId(), "COMMENT", "Title1", "Content", 1L);
        Notification notif2 = new Notification(testUser.getId(), "LIKE", "Title2", "Content", 2L);
        notificationRepository.save(notif1);
        notificationRepository.save(notif2);

        // Act & Assert
        mockMvc.perform(put("/api/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("所有通知已标记为已读"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void markAllAsRead_NoNotifications_Returns200() throws Exception {
        mockMvc.perform(put("/api/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("所有通知已标记为已读"));
    }
}
