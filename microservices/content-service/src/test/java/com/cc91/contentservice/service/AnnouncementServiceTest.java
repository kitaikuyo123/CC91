package com.cc91.contentservice.service;

import com.cc91.contentservice.client.CreateNotificationRequest;
import com.cc91.contentservice.client.NotificationServiceClient;
import com.cc91.contentservice.client.UserInfoDTO;
import com.cc91.contentservice.client.UserServiceClient;
import com.cc91.contentservice.dto.AnnouncementDTO;
import com.cc91.contentservice.dto.CreateAnnouncementRequest;
import com.cc91.contentservice.dto.UpdateAnnouncementRequest;
import com.cc91.contentservice.entity.Announcement;
import com.cc91.contentservice.exception.ResourceNotFoundException;
import com.cc91.contentservice.repository.AnnouncementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AnnouncementService unit tests.
 * Verifies sort order (isPinned DESC + createdAt DESC), Feign author lookup
 * (with degradation), notification best-effort, and CRUD semantics.
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    @InjectMocks
    private AnnouncementService service;

    private Announcement pinnedAnnouncement(Long id, Long authorId) {
        Announcement a = new Announcement("Pinned " + id, "content", authorId);
        a.setId(id);
        a.setIsPinned(true);
        return a;
    }

    private Announcement normalAnnouncement(Long id, Long authorId) {
        Announcement a = new Announcement("Normal " + id, "content", authorId);
        a.setId(id);
        a.setIsPinned(false);
        return a;
    }

    private UserInfoDTO user(Long id, String name) {
        return new UserInfoDTO(id, name, "USER", null);
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should request sort by isPinned DESC then createdAt DESC")
        void shouldUsePinnedThenCreatedAtSort() {
            when(announcementRepository.findAll(any(Sort.class)))
                    .thenReturn(List.of());

            service.findAll();

            ArgumentCaptor<Sort> captor = ArgumentCaptor.forClass(Sort.class);
            verify(announcementRepository).findAll(captor.capture());
            Sort sort = captor.getValue();
            // Order: isPinned DESC then createdAt DESC
            List<Sort.Order> orders = sort.toList();
            assertEquals(2, orders.size());
            assertEquals("isPinned", orders.get(0).getProperty());
            assertTrue(orders.get(0).isDescending());
            assertEquals("createdAt", orders.get(1).getProperty());
            assertTrue(orders.get(1).isDescending());
        }

        @Test
        @DisplayName("should return empty list when repository is empty")
        void shouldReturnEmptyWhenEmpty() {
            when(announcementRepository.findAll(any(Sort.class)))
                    .thenReturn(List.of());
            assertTrue(service.findAll().isEmpty());
        }

        @Test
        @DisplayName("should batch fetch authors via getUsersByIds and map username")
        void shouldBatchFetchAuthors() {
            Announcement a1 = normalAnnouncement(1L, 100L);
            Announcement a2 = normalAnnouncement(2L, 200L);
            when(announcementRepository.findAll(any(Sort.class)))
                    .thenReturn(List.of(a1, a2));
            when(userServiceClient.getUsersByIds(anyList()))
                    .thenReturn(List.of(user(100L, "alice"), user(200L, "bob")));

            List<AnnouncementDTO> result = service.findAll();

            assertEquals(2, result.size());
            assertEquals("alice", result.get(0).getAuthorUsername());
            assertEquals("bob", result.get(1).getAuthorUsername());
        }

        @Test
        @DisplayName("should fall back to '未知用户' when Feign call fails")
        void shouldFallbackAuthorOnFeignFailure() {
            Announcement a1 = normalAnnouncement(1L, 100L);
            when(announcementRepository.findAll(any(Sort.class)))
                    .thenReturn(List.of(a1));
            when(userServiceClient.getUsersByIds(anyList()))
                    .thenThrow(new RuntimeException("network down"));

            List<AnnouncementDTO> result = service.findAll();
            assertEquals(1, result.size());
            assertEquals("未知用户", result.get(0).getAuthorUsername());
            assertEquals(100L, result.get(0).getAuthorId());
        }

        @Test
        @DisplayName("should use '未知用户' for missing author in returned map")
        void shouldFallbackForMissingAuthorInMap() {
            Announcement a1 = normalAnnouncement(1L, 100L);
            when(announcementRepository.findAll(any(Sort.class)))
                    .thenReturn(List.of(a1));
            when(userServiceClient.getUsersByIds(anyList()))
                    .thenReturn(List.of()); // no entry for 100L

            List<AnnouncementDTO> result = service.findAll();
            assertEquals("未知用户", result.get(0).getAuthorUsername());
        }

        @Test
        @DisplayName("should deduplicate author IDs before batch fetch")
        void shouldDeduplicateAuthorIds() {
            Announcement a1 = normalAnnouncement(1L, 100L);
            Announcement a2 = normalAnnouncement(2L, 100L); // same author
            when(announcementRepository.findAll(any(Sort.class)))
                    .thenReturn(List.of(a1, a2));
            when(userServiceClient.getUsersByIds(anyList()))
                    .thenReturn(List.of(user(100L, "alice")));

            service.findAll();

            ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
            verify(userServiceClient).getUsersByIds(captor.capture());
            // dedup'd to a single entry
            assertEquals(1, captor.getValue().size());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(announcementRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> service.findById(999L));
        }

        @Test
        @DisplayName("should fetch single author and return DTO")
        void shouldFetchSingleAuthor() {
            Announcement a = normalAnnouncement(1L, 100L);
            when(announcementRepository.findById(1L)).thenReturn(Optional.of(a));
            when(userServiceClient.getUserById(100L)).thenReturn(user(100L, "alice"));

            AnnouncementDTO dto = service.findById(1L);
            assertEquals(1L, dto.getId());
            assertEquals("alice", dto.getAuthorUsername());
        }

        @Test
        @DisplayName("should fall back to '未知用户' when Feign author fetch throws")
        void shouldFallbackOnFeignFailure() {
            Announcement a = normalAnnouncement(1L, 100L);
            when(announcementRepository.findById(1L)).thenReturn(Optional.of(a));
            when(userServiceClient.getUserById(100L)).thenThrow(new RuntimeException("down"));

            AnnouncementDTO dto = service.findById(1L);
            assertEquals("未知用户", dto.getAuthorUsername());
        }

        @Test
        @DisplayName("should fall back to '未知用户' when Feign returns null username")
        void shouldFallbackWhenUsernameNull() {
            Announcement a = normalAnnouncement(1L, 100L);
            when(announcementRepository.findById(1L)).thenReturn(Optional.of(a));
            when(userServiceClient.getUserById(100L)).thenReturn(new UserInfoDTO(100L, null, "USER", null));

            AnnouncementDTO dto = service.findById(1L);
            assertEquals("未知用户", dto.getAuthorUsername());
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should throw ResourceNotFoundException when author user does not exist")
        void shouldThrowWhenUserMissing() {
            when(userServiceClient.getUserByUsername("ghost")).thenReturn(null);
            CreateAnnouncementRequest req = new CreateAnnouncementRequest("t", "c", false);
            assertThrows(ResourceNotFoundException.class, () -> service.create(req, "ghost"));
            verify(announcementRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when author has null id")
        void shouldThrowWhenUserIdNull() {
            when(userServiceClient.getUserByUsername("ghost"))
                    .thenReturn(new UserInfoDTO(null, "ghost", "USER", null));
            CreateAnnouncementRequest req = new CreateAnnouncementRequest("t", "c", false);
            assertThrows(ResourceNotFoundException.class, () -> service.create(req, "ghost"));
        }

        @Test
        @DisplayName("should save announcement and notify all users on success")
        void shouldSaveAndNotify() {
            UserInfoDTO author = user(100L, "alice");
            when(userServiceClient.getUserByUsername("alice")).thenReturn(author);
            Announcement saved = normalAnnouncement(5L, 100L);
            when(announcementRepository.save(any())).thenReturn(saved);
            when(userServiceClient.getUsersByIds(anyList()))
                    .thenReturn(List.of(user(1L, "u1"), user(2L, "u2")));

            CreateAnnouncementRequest req = new CreateAnnouncementRequest("title", "content", false);
            AnnouncementDTO dto = service.create(req, "alice");

            assertEquals(5L, dto.getId());
            assertEquals("alice", dto.getAuthorUsername());
            verify(notificationServiceClient, times(2)).createNotification(any(CreateNotificationRequest.class));
        }

        @Test
        @DisplayName("should still save when Notification Service fails (best-effort)")
        void shouldSurviveNotificationServiceFailure() {
            UserInfoDTO author = user(100L, "alice");
            when(userServiceClient.getUserByUsername("alice")).thenReturn(author);
            Announcement saved = normalAnnouncement(5L, 100L);
            when(announcementRepository.save(any())).thenReturn(saved);
            when(userServiceClient.getUsersByIds(anyList()))
                    .thenThrow(new RuntimeException("notification service down"));

            CreateAnnouncementRequest req = new CreateAnnouncementRequest("t", "c", false);
            AnnouncementDTO dto = service.create(req, "alice");
            assertEquals(5L, dto.getId());
            // save happened despite notification failure
            verify(announcementRepository).save(any());
        }

        @Test
        @DisplayName("should continue notifying other users when one notification fails")
        void shouldContinueOnPerUserNotificationFailure() {
            UserInfoDTO author = user(100L, "alice");
            when(userServiceClient.getUserByUsername("alice")).thenReturn(author);
            Announcement saved = normalAnnouncement(5L, 100L);
            when(announcementRepository.save(any())).thenReturn(saved);
            when(userServiceClient.getUsersByIds(anyList()))
                    .thenReturn(List.of(user(1L, "u1"), user(2L, "u2")));
            doThrow(new RuntimeException("per-user fail"))
                    .doNothing()
                    .when(notificationServiceClient).createNotification(any());

            CreateAnnouncementRequest req = new CreateAnnouncementRequest("t", "c", false);
            service.create(req, "alice");
            // both attempts made
            verify(notificationServiceClient, times(2)).createNotification(any());
        }

        @Test
        @DisplayName("should set isPinned when request specifies it")
        void shouldSetIsPinnedFromRequest() {
            UserInfoDTO author = user(100L, "alice");
            when(userServiceClient.getUserByUsername("alice")).thenReturn(author);
            Announcement saved = pinnedAnnouncement(5L, 100L);
            when(announcementRepository.save(any())).thenReturn(saved);
            when(userServiceClient.getUsersByIds(anyList())).thenReturn(List.of());

            CreateAnnouncementRequest req = new CreateAnnouncementRequest("t", "c", true);
            AnnouncementDTO dto = service.create(req, "alice");
            // Saved entity should have isPinned = true (verified via returned DTO)
            assertTrue(dto.getIsPinned());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(announcementRepository.findById(999L)).thenReturn(Optional.empty());
            UpdateAnnouncementRequest req = new UpdateAnnouncementRequest();
            assertThrows(ResourceNotFoundException.class, () -> service.update(999L, req));
        }

        @Test
        @DisplayName("should apply partial update (only provided fields)")
        void shouldApplyPartialUpdate() {
            Announcement existing = normalAnnouncement(1L, 100L);
            existing.setTitle("Old");
            existing.setContent("OldContent");
            when(announcementRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(announcementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userServiceClient.getUserById(100L)).thenReturn(user(100L, "alice"));

            UpdateAnnouncementRequest req = new UpdateAnnouncementRequest();
            req.setTitle("New Title");
            // content / isPinned not set
            AnnouncementDTO dto = service.update(1L, req);

            assertEquals("New Title", dto.getTitle());
            assertEquals("OldContent", dto.getContent());
            assertFalse(dto.getIsPinned());
        }

        @Test
        @DisplayName("should update all fields when all provided")
        void shouldUpdateAllFields() {
            Announcement existing = normalAnnouncement(1L, 100L);
            when(announcementRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(announcementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userServiceClient.getUserById(100L)).thenReturn(user(100L, "alice"));

            UpdateAnnouncementRequest req = new UpdateAnnouncementRequest();
            req.setTitle("New");
            req.setContent("NewContent");
            req.setIsPinned(true);
            AnnouncementDTO dto = service.update(1L, req);

            assertEquals("New", dto.getTitle());
            assertEquals("NewContent", dto.getContent());
            assertTrue(dto.getIsPinned());
        }

        @Test
        @DisplayName("should fall back to '未知用户' when author fetch fails")
        void shouldFallbackAuthorOnFeignFailure() {
            Announcement existing = normalAnnouncement(1L, 100L);
            when(announcementRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(announcementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userServiceClient.getUserById(100L)).thenThrow(new RuntimeException("down"));

            UpdateAnnouncementRequest req = new UpdateAnnouncementRequest();
            req.setTitle("New");
            AnnouncementDTO dto = service.update(1L, req);
            assertEquals("未知用户", dto.getAuthorUsername());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(announcementRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> service.delete(999L));
            verify(announcementRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should delete on success")
        void shouldDelete() {
            Announcement existing = normalAnnouncement(1L, 100L);
            when(announcementRepository.findById(1L)).thenReturn(Optional.of(existing));

            service.delete(1L);

            verify(announcementRepository).delete(existing);
        }
    }
}
