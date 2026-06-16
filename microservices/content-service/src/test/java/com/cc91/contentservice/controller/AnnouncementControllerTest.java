package com.cc91.contentservice.controller;

import com.cc91.contentservice.base.BaseWebMvcTest;
import com.cc91.contentservice.dto.AnnouncementDTO;
import com.cc91.contentservice.dto.ApiResponse;
import com.cc91.contentservice.dto.CreateAnnouncementRequest;
import com.cc91.contentservice.dto.UpdateAnnouncementRequest;
import com.cc91.contentservice.exception.ResourceNotFoundException;
import com.cc91.contentservice.service.AnnouncementService;
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
 * AnnouncementController @WebMvcTest.
 *
 * Verifies:
 *  - public GET endpoints are accessible anonymously (SecurityConfig permitAll)
 *  - admin endpoints require ADMIN role; USER → 403 (verifies GlobalExceptionHandler
 *    AccessDeniedException → 403 fix; previously misrouted to 400 by RuntimeException handler)
 *  - validation on CreateAnnouncementRequest
 *  - ResourceNotFoundException → 404 mapping
 */
@WebMvcTest(controllers = AnnouncementController.class)
class AnnouncementControllerTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnnouncementService announcementService;

    private AnnouncementDTO sampleDTO(Long id) {
        return new AnnouncementDTO(id, "title", "content", 100L, "alice", true,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("GET /api/announcements (public)")
    class ListAll {

        @Test
        @DisplayName("should be public (200 without authentication)")
        void shouldBePublic() throws Exception {
            when(announcementService.findAll()).thenReturn(List.of(sampleDTO(1L)));
            mockMvc.perform(get("/api/announcements"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1));
        }

        @Test
        @DisplayName("should return empty array when no announcements")
        void shouldReturnEmptyArray() throws Exception {
            when(announcementService.findAll()).thenReturn(List.of());
            mockMvc.perform(get("/api/announcements"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/announcements/{id} (public)")
    class GetDetail {

        @Test
        @DisplayName("should be public (200 without authentication)")
        void shouldBePublic() throws Exception {
            when(announcementService.findById(1L)).thenReturn(sampleDTO(1L));
            mockMvc.perform(get("/api/announcements/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.authorUsername").value("alice"));
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenMissing() throws Exception {
            when(announcementService.findById(999L))
                    .thenThrow(new ResourceNotFoundException("公告不存在"));
            mockMvc.perform(get("/api/announcements/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/admin/announcements")
    class Create {

        @Test
        @DisplayName("should return 403 when caller is USER (not ADMIN)")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn403ForUser() throws Exception {
            CreateAnnouncementRequest req = new CreateAnnouncementRequest("t", "c", false);
            mockMvc.perform(post("/api/admin/announcements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
            // service MUST NOT be invoked when authorization fails
            verifyNoInteractions(announcementService);
        }

        @Test
        @DisplayName("should return 400 when title is blank")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void shouldReturn400WhenTitleBlank() throws Exception {
            CreateAnnouncementRequest req = new CreateAnnouncementRequest("", "c", false);
            mockMvc.perform(post("/api/admin/announcements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when content is blank")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void shouldReturn400WhenContentBlank() throws Exception {
            CreateAnnouncementRequest req = new CreateAnnouncementRequest("t", "", false);
            mockMvc.perform(post("/api/admin/announcements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when title exceeds 200 chars")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void shouldReturn400WhenTitleTooLong() throws Exception {
            String longTitle = "x".repeat(201);
            CreateAnnouncementRequest req = new CreateAnnouncementRequest(longTitle, "c", false);
            mockMvc.perform(post("/api/admin/announcements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 200 + ApiResponse for ADMIN on success")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void shouldReturn200ForAdmin() throws Exception {
            when(announcementService.create(any(), eq("admin"))).thenReturn(sampleDTO(5L));
            CreateAnnouncementRequest req = new CreateAnnouncementRequest("t", "c", false);
            mockMvc.perform(post("/api/admin/announcements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(5));
        }

        @Test
        @DisplayName("should return 404 when author user does not exist")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void shouldReturn404WhenAuthorMissing() throws Exception {
            when(announcementService.create(any(), anyString()))
                    .thenThrow(new ResourceNotFoundException("用户不存在"));
            CreateAnnouncementRequest req = new CreateAnnouncementRequest("t", "c", false);
            mockMvc.perform(post("/api/admin/announcements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/announcements/{id}")
    class Update {

        @Test
        @DisplayName("should return 403 when caller is USER")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn403ForUser() throws Exception {
            UpdateAnnouncementRequest req = new UpdateAnnouncementRequest();
            req.setTitle("t");
            mockMvc.perform(put("/api/admin/announcements/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(announcementService);
        }

        @Test
        @DisplayName("should return 400 when title exceeds 200 chars")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void shouldReturn400WhenTitleTooLong() throws Exception {
            UpdateAnnouncementRequest req = new UpdateAnnouncementRequest();
            req.setTitle("x".repeat(201));
            mockMvc.perform(put("/api/admin/announcements/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 200 on successful update for ADMIN")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void shouldReturn200ForAdmin() throws Exception {
            when(announcementService.update(eq(1L), any())).thenReturn(sampleDTO(1L));
            UpdateAnnouncementRequest req = new UpdateAnnouncementRequest();
            req.setTitle("new title");
            mockMvc.perform(put("/api/admin/announcements/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should return 404 when not found")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void shouldReturn404WhenNotFound() throws Exception {
            when(announcementService.update(eq(999L), any()))
                    .thenThrow(new ResourceNotFoundException("公告不存在"));
            UpdateAnnouncementRequest req = new UpdateAnnouncementRequest();
            req.setTitle("new");
            mockMvc.perform(put("/api/admin/announcements/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/announcements/{id}")
    class Delete {

        @Test
        @DisplayName("should return 403 when caller is USER")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn403ForUser() throws Exception {
            mockMvc.perform(delete("/api/admin/announcements/1"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(announcementService);
        }

        @Test
        @DisplayName("should return 200 on success for ADMIN")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void shouldReturn200ForAdmin() throws Exception {
            doNothing().when(announcementService).delete(1L);
            mockMvc.perform(delete("/api/admin/announcements/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should return 404 (not 500) when not found")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void shouldReturn404WhenNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("公告不存在"))
                    .when(announcementService).delete(999L);
            mockMvc.perform(delete("/api/admin/announcements/999"))
                    .andExpect(status().isNotFound());
        }
    }
}
