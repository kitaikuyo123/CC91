package com.cc91.notificationservice.repository;

import com.cc91.notificationservice.entity.Notification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NotificationRepository @DataJpaTest against H2.
 *
 * IMPORTANT: @DataJpaTest wraps each test method in a transaction that is rolled
 * back at the end. For @Modifying queries we still need to flush and clear
 * (handled by clearAutomatically=true on the @Modifying annotation). After invoking
 * markAllAsReadByUserId we use TestEntityManager.findFresh / clear() to ensure
 * the in-memory cache reflects the bulk update.
 */
@DataJpaTest
@ActiveProfiles("test")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Notification persist(Long userId, boolean isRead, LocalDateTime createdAt) {
        Notification n = new Notification(userId, "SYSTEM", "title", "content", null);
        n.setIsRead(isRead);
        n.setCreatedAt(createdAt);
        entityManager.persist(n);
        entityManager.flush();
        return n;
    }

    @Nested
    @DisplayName("findByUserIdOrderByCreatedAtDesc")
    class FindByUserId {

        @Test
        @DisplayName("should return only the user's notifications ordered by createdAt DESC")
        void shouldFilterByUserAndSortDesc() {
            Notification older = persist(100L, false, LocalDateTime.of(2024, 1, 1, 10, 0));
            Notification newer = persist(100L, false, LocalDateTime.of(2024, 1, 5, 10, 0));
            // other user's notification must be excluded
            persist(200L, false, LocalDateTime.of(2024, 1, 3, 10, 0));

            entityManager.clear();

            Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                    100L, PageRequest.of(0, 10));

            assertEquals(2, page.getTotalElements());
            // newer first
            assertEquals(newer.getId(), page.getContent().get(0).getId());
            assertEquals(older.getId(), page.getContent().get(1).getId());
        }

        @Test
        @DisplayName("should paginate the user's notifications")
        void shouldPaginate() {
            for (int i = 0; i < 5; i++) {
                persist(100L, false, LocalDateTime.of(2024, 1, 1, 10, i));
            }
            entityManager.clear();

            Page<Notification> page1 = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                    100L, PageRequest.of(0, 2));
            Page<Notification> page2 = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                    100L, PageRequest.of(1, 2));

            assertEquals(5, page1.getTotalElements());
            assertEquals(2, page1.getNumberOfElements());
            assertEquals(2, page2.getNumberOfElements());
        }

        @Test
        @DisplayName("should return empty page when user has no notifications")
        void shouldReturnEmptyForUnknownUser() {
            Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                    999L, PageRequest.of(0, 10));
            assertEquals(0, page.getTotalElements());
        }

        @Test
        @DisplayName("should support List (unpaginated) overload")
        void shouldSupportUnpaginatedOverload() {
            persist(100L, false, LocalDateTime.now());
            persist(100L, true, LocalDateTime.now());
            persist(200L, false, LocalDateTime.now());

            var list = notificationRepository.findByUserIdOrderByCreatedAtDesc(100L);
            assertEquals(2, list.size());
            list.forEach(n -> assertEquals(100L, n.getUserId()));
        }
    }

    @Nested
    @DisplayName("countByUserIdAndIsReadFalse")
    class CountUnread {

        @Test
        @DisplayName("should count only unread notifications for the user")
        void shouldCountUnreadForUser() {
            persist(100L, false, LocalDateTime.now());
            persist(100L, false, LocalDateTime.now());
            persist(100L, true, LocalDateTime.now());
            persist(200L, false, LocalDateTime.now()); // other user

            Long count = notificationRepository.countByUserIdAndIsReadFalse(100L);
            assertEquals(2L, count);
        }

        @Test
        @DisplayName("should return 0 when no unread")
        void shouldReturnZeroWhenNoUnread() {
            persist(100L, true, LocalDateTime.now());

            Long count = notificationRepository.countByUserIdAndIsReadFalse(100L);
            assertEquals(0L, count);
        }

        @Test
        @DisplayName("should return 0 when user has no notifications at all")
        void shouldReturnZeroForUnknownUser() {
            Long count = notificationRepository.countByUserIdAndIsReadFalse(999L);
            assertEquals(0L, count);
        }
    }

    @Nested
    @DisplayName("markAllAsReadByUserId (@Modifying batch UPDATE)")
    class MarkAllAsRead {

        @Test
        @DisplayName("should set isRead=true for all unread notifications of the user")
        void shouldMarkAllAsReadForUser() {
            Notification n1 = persist(100L, false, LocalDateTime.now());
            Notification n2 = persist(100L, false, LocalDateTime.now());
            Notification n3 = persist(100L, true, LocalDateTime.now()); // already read
            // other user — must be untouched
            Notification otherUnread = persist(200L, false, LocalDateTime.now());

            // @Modifying(clearAutomatically=true, flushAutomatically=true) flushes & clears.
            notificationRepository.markAllAsReadByUserId(100L);

            // After clearAutomatically the persistence context is empty → fresh fetches.
            assertEquals(true, entityManager.find(Notification.class, n1.getId()).getIsRead());
            assertEquals(true, entityManager.find(Notification.class, n2.getId()).getIsRead());
            assertEquals(true, entityManager.find(Notification.class, n3.getId()).getIsRead());
            // other user's notification stays unread
            assertEquals(false, entityManager.find(Notification.class, otherUnread.getId()).getIsRead());
        }

        @Test
        @DisplayName("should reduce unread count to 0 after batch update")
        void shouldReduceUnreadCountToZero() {
            persist(100L, false, LocalDateTime.now());
            persist(100L, false, LocalDateTime.now());
            persist(100L, false, LocalDateTime.now());

            assertEquals(3L, notificationRepository.countByUserIdAndIsReadFalse(100L));

            notificationRepository.markAllAsReadByUserId(100L);

            assertEquals(0L, notificationRepository.countByUserIdAndIsReadFalse(100L));
        }

        @Test
        @DisplayName("should be idempotent (running twice is a no-op the second time)")
        void shouldBeIdempotent() {
            persist(100L, false, LocalDateTime.now());

            notificationRepository.markAllAsReadByUserId(100L);
            assertEquals(0L, notificationRepository.countByUserIdAndIsReadFalse(100L));

            // second call must not error and must not change anything
            notificationRepository.markAllAsReadByUserId(100L);
            assertEquals(0L, notificationRepository.countByUserIdAndIsReadFalse(100L));
        }

        @Test
        @DisplayName("should not affect other users")
        void shouldNotAffectOtherUsers() {
            persist(100L, false, LocalDateTime.now());
            persist(200L, false, LocalDateTime.now());
            persist(200L, false, LocalDateTime.now());

            notificationRepository.markAllAsReadByUserId(100L);

            assertEquals(0L, notificationRepository.countByUserIdAndIsReadFalse(100L));
            assertEquals(2L, notificationRepository.countByUserIdAndIsReadFalse(200L));
        }
    }
}
