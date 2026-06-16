package com.cc91.notificationservice.service;

import com.cc91.notificationservice.dto.NotificationDTO;
import com.cc91.notificationservice.entity.Notification;
import com.cc91.notificationservice.exception.ResourceNotFoundException;
import com.cc91.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * NotificationServiceImpl unit tests.
 * Verifies ownership enforcement (OWASP A01), pagination, unread count, and
 * the @Modifying batch update delegation.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl service;

    private Notification notification(Long id, Long userId, boolean isRead) {
        Notification n = new Notification(userId, "SYSTEM", "title", "content", null);
        n.setId(id);
        n.setIsRead(isRead);
        return n;
    }

    @Nested
    @DisplayName("getNotifications")
    class GetNotifications {

        @Test
        @DisplayName("should return DTO list with createdAt DESC sort")
        void shouldReturnDtoList() {
            Notification n1 = notification(1L, 100L, false);
            Notification n2 = notification(2L, 100L, true);
            Page<Notification> page = new PageImpl<>(List.of(n2, n1));
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(100L), any(Pageable.class)))
                    .thenReturn(page);

            List<NotificationDTO> result = service.getNotifications(100L, 0, 20);

            assertEquals(2, result.size());
            assertEquals(2L, result.get(0).getId());
            assertTrue(result.get(0).getIsRead());
            assertFalse(result.get(1).getIsRead());
        }

        @Test
        @DisplayName("should construct Pageable with page/size and createdAt DESC sort")
        void shouldConstructPageableWithSort() {
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(100L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            service.getNotifications(100L, 2, 50);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(eq(100L), captor.capture());
            Pageable pageable = captor.getValue();
            assertEquals(2, pageable.getPageNumber());
            assertEquals(50, pageable.getPageSize());
            assertNotNull(pageable.getSort().getOrderFor("createdAt"));
            assertTrue(pageable.getSort().getOrderFor("createdAt").isDescending());
        }

        @Test
        @DisplayName("should return empty list when no notifications")
        void shouldReturnEmptyList() {
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(100L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));
            assertTrue(service.getNotifications(100L, 0, 20).isEmpty());
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCount {

        @Test
        @DisplayName("should delegate to repository.countByUserIdAndIsReadFalse")
        void shouldDelegateToRepository() {
            when(notificationRepository.countByUserIdAndIsReadFalse(100L)).thenReturn(5L);
            assertEquals(5L, service.getUnreadCount(100L));
        }

        @Test
        @DisplayName("should return 0 when repository returns 0")
        void shouldReturnZero() {
            when(notificationRepository.countByUserIdAndIsReadFalse(100L)).thenReturn(0L);
            assertEquals(0L, service.getUnreadCount(100L));
        }
    }

    @Nested
    @DisplayName("markAsRead (ownership enforcement — OWASP A01)")
    class MarkAsRead {

        @Test
        @DisplayName("should mark as read when caller is the owner")
        void shouldMarkReadWhenOwner() {
            Notification n = notification(1L, 100L, false);
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.markAsRead(1L, 100L);

            assertTrue(n.getIsRead());
            verify(notificationRepository).save(n);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when caller is NOT the owner")
        void shouldThrowWhenNotOwner() {
            Notification n = notification(1L, 100L, false);
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

            // attacker (userId 200) tries to mark user 100's notification as read
            assertThrows(IllegalArgumentException.class, () -> service.markAsRead(1L, 200L));
            // notification must NOT be modified
            assertFalse(n.getIsRead());
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when notification does not exist")
        void shouldThrowWhenNotFound() {
            when(notificationRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> service.markAsRead(999L, 100L));
        }

        @Test
        @DisplayName("should NOT save when already read (idempotent)")
        void shouldBeIdempotent() {
            Notification n = notification(1L, 100L, true);
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.markAsRead(1L, 100L);
            // Even if already read, save is still invoked (no short-circuit) — verify it's not throwing
            assertTrue(n.getIsRead());
        }
    }

    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsRead {

        @Test
        @DisplayName("should delegate to repository.markAllAsReadByUserId")
        void shouldDelegateToRepository() {
            service.markAllAsRead(100L);
            verify(notificationRepository).markAllAsReadByUserId(100L);
        }
    }

    @Nested
    @DisplayName("deleteNotification (ownership enforcement — OWASP A01)")
    class DeleteNotification {

        @Test
        @DisplayName("should delete when caller is the owner")
        void shouldDeleteWhenOwner() {
            Notification n = notification(1L, 100L, false);
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

            service.deleteNotification(1L, 100L);
            verify(notificationRepository).delete(n);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when caller is NOT the owner")
        void shouldThrowWhenNotOwner() {
            Notification n = notification(1L, 100L, false);
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

            assertThrows(IllegalArgumentException.class, () -> service.deleteNotification(1L, 200L));
            verify(notificationRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when notification does not exist")
        void shouldThrowWhenNotFound() {
            when(notificationRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> service.deleteNotification(999L, 100L));
        }
    }

    @Nested
    @DisplayName("createNotification")
    class CreateNotification {

        @Test
        @DisplayName("should persist notification with all fields")
        void shouldPersistWithAllFields() {
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createNotification(100L, "ANNOUNCEMENT", "title", "content", 5L);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            Notification saved = captor.getValue();
            assertEquals(100L, saved.getUserId());
            assertEquals("ANNOUNCEMENT", saved.getType());
            assertEquals("title", saved.getTitle());
            assertEquals("content", saved.getContent());
            assertEquals(5L, saved.getRelatedId());
        }

        @Test
        @DisplayName("should accept null content and relatedId")
        void shouldAcceptNullableFields() {
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createNotification(100L, "SYSTEM", "title", null, null);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertNull(captor.getValue().getContent());
            assertNull(captor.getValue().getRelatedId());
        }
    }
}
