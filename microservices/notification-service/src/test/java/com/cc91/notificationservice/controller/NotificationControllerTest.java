package com.cc91.notificationservice.controller;

import com.cc91.notificationservice.base.BaseWebMvcTest;
import com.cc91.notificationservice.client.UserServiceClient;
import com.cc91.notificationservice.dto.CreateNotificationRequest;
import com.cc91.notificationservice.dto.NotificationDTO;
import com.cc91.notificationservice.exception.ResourceNotFoundException;
import com.cc91.notificationservice.service.NotificationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * NotificationController @WebMvcTest.
 * The controller reads userId directly from the JWT (Authorization header) via
 * jwtUtil.getUserIdFromToken — it does not rely on SecurityContext. To drive the
 * 6 endpoints we therefore use @WithMockUser (satisfies .anyRequest().authenticated())
 * and stub jwtUtil.getUserIdFromToken to return the caller's id.
 *
 * Note: anonymous-access (401) is enforced by SecurityConfig and verified at
 * integration level; not reliably reproducible in @WebMvcTest slice.
 */
@WebMvcTest(controllers = NotificationController.class)
class NotificationControllerTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationServiceImpl notificationService;

    @MockBean
    private UserServiceClient userServiceClient;

    private static final String AUTH_HEADER = "Bearer fake.jwt.token";
    private static final Long CALLER_USER_ID = 100L;

    private NotificationDTO sampleDTO(Long id, Long userId, boolean isRead) {
        return new NotificationDTO(id, userId, "SYSTEM", "title", "content", null,
                isRead, LocalDateTime.now());
    }

    /**
     * Sets up jwtUtil mock to return CALLER_USER_ID for the fake token.
     * Tests must invoke this in their setup; we cannot use @BeforeEach for the
     * whole class because some tests need different userIds (cross-user / 403).
     */
    private void callerIs(Long userId) {
        when(jwtUtil.getUserIdFromToken("fake.jwt.token")).thenReturn(userId);
    }

    @Nested
    @DisplayName("GET /api/notifications")
    class GetNotifications {

        @Test
        @DisplayName("should return list of notifications for authenticated user")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturnListForAuthenticatedUser() throws Exception {
            callerIs(CALLER_USER_ID);
            when(notificationService.getNotifications(eq(CALLER_USER_ID), eq(0), eq(20)))
                    .thenReturn(List.of(sampleDTO(1L, CALLER_USER_ID, false)));

            mockMvc.perform(get("/api/notifications").header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1));
        }

        @Test
        @DisplayName("should accept custom page/size params")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldAcceptCustomPagination() throws Exception {
            callerIs(CALLER_USER_ID);
            when(notificationService.getNotifications(eq(CALLER_USER_ID), eq(2), eq(50)))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/notifications")
                            .param("page", "2")
                            .param("size", "50")
                            .header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk());
            verify(notificationService).getNotifications(eq(CALLER_USER_ID), eq(2), eq(50));
        }

        @Test
        @DisplayName("should return 400 when page is negative")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn400WhenPageNegative() throws Exception {
            callerIs(CALLER_USER_ID);
            mockMvc.perform(get("/api/notifications")
                            .param("page", "-1")
                            .header("Authorization", AUTH_HEADER))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when size is 0")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn400WhenSizeZero() throws Exception {
            callerIs(CALLER_USER_ID);
            mockMvc.perform(get("/api/notifications")
                            .param("size", "0")
                            .header("Authorization", AUTH_HEADER))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/notifications/unread-count")
    class GetUnreadCount {

        @Test
        @DisplayName("should return numeric count for authenticated user")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturnCount() throws Exception {
            callerIs(CALLER_USER_ID);
            when(notificationService.getUnreadCount(CALLER_USER_ID)).thenReturn(7L);
            mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk())
                    .andExpect(content().string("7"));
        }
    }

    @Nested
    @DisplayName("PUT /api/notifications/{id}/read")
    class MarkAsRead {

        @Test
        @DisplayName("should return 200 + ApiResponse when caller is owner")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn200WhenOwner() throws Exception {
            callerIs(CALLER_USER_ID);
            doNothing().when(notificationService).markAsRead(eq(1L), eq(CALLER_USER_ID));
            mockMvc.perform(put("/api/notifications/1/read").header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk());
            verify(notificationService).markAsRead(1L, CALLER_USER_ID);
        }

        @Test
        @DisplayName("should return 403 when caller is NOT the owner (cross-user attack)")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn403WhenNotOwner() throws Exception {
            callerIs(CALLER_USER_ID);
            // attacker (caller) tries to mark user 200's notification
            doThrow(new IllegalArgumentException("无权限操作此通知"))
                    .when(notificationService).markAsRead(eq(5L), eq(CALLER_USER_ID));
            mockMvc.perform(put("/api/notifications/5/read").header("Authorization", AUTH_HEADER))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when notification does not exist")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn404WhenNotFound() throws Exception {
            callerIs(CALLER_USER_ID);
            doThrow(new ResourceNotFoundException("通知不存在"))
                    .when(notificationService).markAsRead(eq(999L), eq(CALLER_USER_ID));
            mockMvc.perform(put("/api/notifications/999/read").header("Authorization", AUTH_HEADER))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/notifications/read-all")
    class MarkAllAsRead {

        @Test
        @DisplayName("should return 200 + ApiResponse on success")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn200OnSuccess() throws Exception {
            callerIs(CALLER_USER_ID);
            doNothing().when(notificationService).markAllAsRead(CALLER_USER_ID);
            mockMvc.perform(put("/api/notifications/read-all").header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk());
            // verify the service was called — that's the key assertion; response body
            // content is checked in the GET endpoints which return data
            verify(notificationService).markAllAsRead(CALLER_USER_ID);
        }
    }

    @Nested
    @DisplayName("DELETE /api/notifications/{id}")
    class DeleteNotification {

        @Test
        @DisplayName("should return 200 when caller is owner")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn200WhenOwner() throws Exception {
            callerIs(CALLER_USER_ID);
            doNothing().when(notificationService).deleteNotification(eq(1L), eq(CALLER_USER_ID));
            mockMvc.perform(delete("/api/notifications/1").header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk());
            verify(notificationService).deleteNotification(1L, CALLER_USER_ID);
        }

        @Test
        @DisplayName("should return 403 when caller is NOT the owner")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn403WhenNotOwner() throws Exception {
            callerIs(CALLER_USER_ID);
            doThrow(new IllegalArgumentException("无权限删除此通知"))
                    .when(notificationService).deleteNotification(eq(5L), eq(CALLER_USER_ID));
            mockMvc.perform(delete("/api/notifications/5").header("Authorization", AUTH_HEADER))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when notification does not exist")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn404WhenNotFound() throws Exception {
            callerIs(CALLER_USER_ID);
            doThrow(new ResourceNotFoundException("通知不存在"))
                    .when(notificationService).deleteNotification(eq(999L), eq(CALLER_USER_ID));
            mockMvc.perform(delete("/api/notifications/999").header("Authorization", AUTH_HEADER))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/notifications/internal")
    class CreateNotificationInternal {

        private static final String INTERNAL_TOKEN = "test-internal-token-cc91";

        @Test
        @DisplayName("should return 401 when X-Internal-Token is missing (OWASP A01)")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn401WhenTokenMissing() throws Exception {
            CreateNotificationRequest req = new CreateNotificationRequest(
                    1L, "SYSTEM", "t", "c", null);
            mockMvc.perform(post("/api/notifications/internal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
            verify(notificationService, never()).createNotification(anyLong(), anyString(),
                    anyString(), any(), any());
        }

        @Test
        @DisplayName("should return 401 when X-Internal-Token is wrong")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn401WhenTokenWrong() throws Exception {
            CreateNotificationRequest req = new CreateNotificationRequest(
                    1L, "SYSTEM", "t", "c", null);
            mockMvc.perform(post("/api/notifications/internal")
                            .header("X-Internal-Token", "wrong-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
            verify(notificationService, never()).createNotification(anyLong(), anyString(),
                    anyString(), any(), any());
        }

        @Test
        @DisplayName("should return 400 when userId is missing (with valid internal token)")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn400WhenUserIdMissing() throws Exception {
            CreateNotificationRequest req = new CreateNotificationRequest();
            req.setType("SYSTEM");
            req.setTitle("t");
            mockMvc.perform(post("/api/notifications/internal")
                            .header("X-Internal-Token", INTERNAL_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when type is missing (with valid internal token)")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn400WhenTypeMissing() throws Exception {
            CreateNotificationRequest req = new CreateNotificationRequest();
            req.setUserId(1L);
            req.setTitle("t");
            mockMvc.perform(post("/api/notifications/internal")
                            .header("X-Internal-Token", INTERNAL_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when title is missing (with valid internal token)")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn400WhenTitleMissing() throws Exception {
            CreateNotificationRequest req = new CreateNotificationRequest();
            req.setUserId(1L);
            req.setType("SYSTEM");
            mockMvc.perform(post("/api/notifications/internal")
                            .header("X-Internal-Token", INTERNAL_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 200 + ApiResponse.success=true on success")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn200OnSuccess() throws Exception {
            doNothing().when(notificationService).createNotification(anyLong(), anyString(),
                    anyString(), any(), any());
            CreateNotificationRequest req = new CreateNotificationRequest(
                    1L, "SYSTEM", "t", "c", null);
            mockMvc.perform(post("/api/notifications/internal")
                            .header("X-Internal-Token", INTERNAL_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
            verify(notificationService).createNotification(eq(1L), eq("SYSTEM"),
                    eq("t"), eq("c"), eq(null));
        }
    }
}
