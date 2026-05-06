package com.cc91.service;

import com.cc91.dto.NotificationDTO;
import com.cc91.entity.Notification;
import com.cc91.entity.User;
import com.cc91.exception.ResourceNotFoundException;
import com.cc91.repository.NotificationRepository;
import com.cc91.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NotificationService business logic tests
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Long testUserId;

    @BeforeEach
    void cleanDatabase() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        testUser = userRepository.save(testUser);
        testUserId = testUser.getId();
    }

    // ==================== getNotifications ====================

    @Test
    @Transactional
    void getNotifications_ReturnsUserNotifications() {
        // Arrange
        Notification notif1 = new Notification(testUserId, "COMMENT", "New comment", "Content", 1L);
        Notification notif2 = new Notification(testUserId, "LIKE", "New like", "Content", 2L);
        notificationRepository.save(notif1);
        notificationRepository.save(notif2);

        // Act
        List<NotificationDTO> notifications = notificationService.getNotifications(testUserId, 0, 10);

        // Assert
        assertNotNull(notifications);
        assertEquals(2, notifications.size());
    }

    @Test
    void getNotifications_UserNotExists_ThrowsResourceNotFoundException() {
        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class,
                () -> notificationService.getNotifications(999L, 0, 10));
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    @Transactional
    void getNotifications_Paginated_ReturnsCorrectPage() {
        // Arrange: create 5 notifications
        for (int i = 0; i < 5; i++) {
            Notification notif = new Notification(testUserId, "TEST", "Title " + i, "Content", 1L);
            notificationRepository.save(notif);
        }

        // Act: get first page with size 2
        List<NotificationDTO> page1 = notificationService.getNotifications(testUserId, 0, 2);
        List<NotificationDTO> page2 = notificationService.getNotifications(testUserId, 1, 2);

        // Assert
        assertEquals(2, page1.size());
        assertEquals(2, page2.size());
    }

    // ==================== getUnreadCount ====================

    @Test
    @Transactional
    void getUnreadCount_ReturnsCorrectCount() {
        // Arrange
        Notification notif1 = new Notification(testUserId, "COMMENT", "Title", "Content", 1L);
        Notification notif2 = new Notification(testUserId, "LIKE", "Title", "Content", 2L);
        notif2.setIsRead(true);
        notificationRepository.save(notif1);
        notificationRepository.save(notif2);

        // Act
        Long count = notificationService.getUnreadCount(testUserId);

        // Assert
        assertEquals(1L, count);
    }

    @Test
    void getUnreadCount_UserNotExists_ThrowsResourceNotFoundException() {
        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class,
                () -> notificationService.getUnreadCount(999L));
        assertEquals("用户不存在", exception.getMessage());
    }

    // ==================== markAsRead ====================

    @Test
    @Transactional
    void markAsRead_ValidNotification_MarksAsRead() {
        // Arrange
        Notification notif = new Notification(testUserId, "COMMENT", "Title", "Content", 1L);
        notif = notificationRepository.save(notif);
        Long notifId = notif.getId();

        // Act
        notificationService.markAsRead(notifId, testUserId);

        // Assert
        Notification updated = notificationRepository.findById(notifId).orElse(null);
        assertNotNull(updated);
        assertTrue(updated.getIsRead());
    }

    @Test
    void markAsRead_NotificationNotExists_ThrowsResourceNotFoundException() {
        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAsRead(999L, testUserId));
        assertEquals("通知不存在", exception.getMessage());
    }

    @Test
    @Transactional
    void markAsRead_WrongUser_ThrowsIllegalArgumentException() {
        // Arrange: create another user
        User otherUser = new User("other", "other@example.com", passwordEncoder.encode("pass"));
        otherUser = userRepository.save(otherUser);
        Long otherUserId = otherUser.getId();

        Notification notif = new Notification(otherUserId, "COMMENT", "Title", "Content", 1L);
        notif = notificationRepository.save(notif);
        Long notifId = notif.getId();

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> notificationService.markAsRead(notifId, testUserId));
        assertEquals("无权限操作此通知", exception.getMessage());
    }

    // ==================== markAllAsRead ====================

    @Test
    @Transactional
    void markAllAsRead_MarksAllUnreadAsRead() {
        // Arrange
        Notification notif1 = new Notification(testUserId, "COMMENT", "Title1", "Content", 1L);
        Notification notif2 = new Notification(testUserId, "LIKE", "Title2", "Content", 2L);
        Notification notif3 = new Notification(testUserId, "FOLLOW", "Title3", "Content", 3L);
        notif3.setIsRead(true);
        notificationRepository.save(notif1);
        notificationRepository.save(notif2);
        notificationRepository.save(notif3);

        // Act
        notificationService.markAllAsRead(testUserId);

        // Assert
        List<Notification> all = notificationRepository.findByUserIdOrderByCreatedAtDesc(testUserId);
        assertTrue(all.stream().allMatch(Notification::getIsRead));
    }

    @Test
    void markAllAsRead_UserNotExists_ThrowsResourceNotFoundException() {
        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAllAsRead(999L));
        assertEquals("用户不存在", exception.getMessage());
    }

    // ==================== createNotification ====================

    @Test
    @Transactional
    void createNotification_SavesToDatabase() {
        // Act
        notificationService.createNotification(testUserId, "COMMENT", "New comment", "Content", 1L);

        // Assert
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(testUserId);
        assertEquals(1, notifications.size());
        Notification notif = notifications.get(0);
        assertEquals("COMMENT", notif.getType());
        assertEquals("New comment", notif.getTitle());
        assertEquals("Content", notif.getContent());
        assertEquals(1L, notif.getRelatedId());
        assertFalse(notif.getIsRead());
    }

    @Test
    @Transactional
    void createNotification_MultipleNotifications_AllSaved() {
        // Act
        notificationService.createNotification(testUserId, "COMMENT", "Title1", "Content1", 1L);
        notificationService.createNotification(testUserId, "LIKE", "Title2", "Content2", 2L);
        notificationService.createNotification(testUserId, "FOLLOW", "Title3", "Content3", 3L);

        // Assert
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(testUserId);
        assertEquals(3, notifications.size());
    }
}
