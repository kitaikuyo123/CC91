package com.cc91.contentservice.controller;

import com.cc91.contentservice.base.BaseWebMvcTest;
import com.cc91.contentservice.dto.ApiResponse;
import com.cc91.contentservice.dto.CreateReportRequest;
import com.cc91.contentservice.entity.Report;
import com.cc91.contentservice.exception.BadRequestException;
import com.cc91.contentservice.exception.ResourceNotFoundException;
import com.cc91.contentservice.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ReportController @WebMvcTest.
 * Verifies auth rules, validation, status flow, and @PreAuthorize behaviour.
 *
 * Note: anonymous-access (401) is enforced by SecurityConfig and verified at
 * integration level; not reliably reproducible in @WebMvcTest slice (same
 * limitation documented in forum-service).
 */
@WebMvcTest(controllers = ReportController.class)
class ReportControllerTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReportService reportService;

    private Report sampleReport(Long id) {
        Report r = new Report(100L, Report.TargetType.POST, 1L, "spam");
        r.setId(id);
        return r;
    }

    @Nested
    @DisplayName("POST /api/reports")
    class CreateReport {

        @Test
        @DisplayName("should return 200 + ApiResponse for authenticated USER")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn200ForAuthenticatedUser() throws Exception {
            when(reportService.createReport(eq("alice"), eq(1L), eq("POST"),
                    anyString(), any())).thenReturn(sampleReport(1L));
            CreateReportRequest req = new CreateReportRequest();
            req.setContentId(1L);
            req.setContentType("POST");
            req.setReason("spam");
            mockMvc.perform(post("/api/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should return 400 when contentId is missing")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn400WhenContentIdMissing() throws Exception {
            CreateReportRequest req = new CreateReportRequest();
            req.setContentType("POST");
            req.setReason("spam");
            mockMvc.perform(post("/api/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when contentType is missing")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn400WhenContentTypeMissing() throws Exception {
            CreateReportRequest req = new CreateReportRequest();
            req.setContentId(1L);
            req.setReason("spam");
            mockMvc.perform(post("/api/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when contentType is invalid (not POST/COMMENT)")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn400WhenContentTypeInvalid() throws Exception {
            CreateReportRequest req = new CreateReportRequest();
            req.setContentId(1L);
            req.setContentType("USER");
            req.setReason("spam");
            mockMvc.perform(post("/api/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when reason is blank")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn400WhenReasonBlank() throws Exception {
            CreateReportRequest req = new CreateReportRequest();
            req.setContentId(1L);
            req.setContentType("POST");
            req.setReason("");
            mockMvc.perform(post("/api/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 404 when reporting user does not exist")
        @WithMockUser(username = "ghost", roles = "USER")
        void shouldReturn404WhenUserMissing() throws Exception {
            when(reportService.createReport(anyString(), anyLong(), anyString(), anyString(), any()))
                    .thenThrow(new ResourceNotFoundException("用户不存在"));
            CreateReportRequest req = new CreateReportRequest();
            req.setContentId(1L);
            req.setContentType("POST");
            req.setReason("spam");
            mockMvc.perform(post("/api/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should accept optional description up to 500 chars")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldAcceptDescription() throws Exception {
            when(reportService.createReport(eq("alice"), eq(1L), eq("POST"),
                    eq("spam"), eq("more details"))).thenReturn(sampleReport(1L));
            CreateReportRequest req = new CreateReportRequest();
            req.setContentId(1L);
            req.setContentType("POST");
            req.setReason("spam");
            req.setDescription("more details");
            mockMvc.perform(post("/api/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/reports")
    class GetReports {

        @Test
        @DisplayName("should return 403 when caller is USER")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn403ForUser() throws Exception {
            mockMvc.perform(get("/api/admin/reports"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(reportService);
        }

        @Test
        @DisplayName("should return 200 + list for ADMIN without status filter")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void shouldReturn200ForAdminWithoutStatus() throws Exception {
            Page<Report> page = new PageImpl<>(List.of(sampleReport(1L), sampleReport(2L)),
                    PageRequest.of(0, 1000), 2);
            when(reportService.getReports(isNull(), eq(0), eq(1000))).thenReturn(page);
            mockMvc.perform(get("/api/admin/reports"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("should pass status filter to service")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void shouldPassStatusFilter() throws Exception {
            Page<Report> page = new PageImpl<>(List.of());
            when(reportService.getReports(eq("PENDING"), eq(0), eq(1000))).thenReturn(page);
            mockMvc.perform(get("/api/admin/reports").param("status", "PENDING"))
                    .andExpect(status().isOk());
            verify(reportService).getReports(eq("PENDING"), eq(0), eq(1000));
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/reports/{id}")
    class HandleReport {

        @Test
        @DisplayName("should return 403 when caller is USER")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn403ForUser() throws Exception {
            String body = "{\"status\":\"RESOLVED\"}";
            mockMvc.perform(put("/api/admin/reports/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(reportService);
        }

        @Test
        @DisplayName("should return 400 when status is null/blank in request body")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void shouldReturn400WhenStatusBlank() throws Exception {
            String body = "{\"status\":\"\"}";
            mockMvc.perform(put("/api/admin/reports/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 200 + ApiResponse on success")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void shouldReturn200OnSuccess() throws Exception {
            Report resolved = sampleReport(1L);
            resolved.setStatus(Report.ReportStatus.RESOLVED);
            when(reportService.handleReport(eq(1L), eq("RESOLVED"), eq("ok")))
                    .thenReturn(resolved);
            String body = "{\"status\":\"RESOLVED\",\"adminComment\":\"ok\"}";
            mockMvc.perform(put("/api/admin/reports/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should return 404 when report does not exist")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void shouldReturn404WhenNotFound() throws Exception {
            when(reportService.handleReport(eq(999L), anyString(), any()))
                    .thenThrow(new ResourceNotFoundException("举报不存在"));
            String body = "{\"status\":\"RESOLVED\"}";
            mockMvc.perform(put("/api/admin/reports/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when status is invalid")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void shouldReturn400WhenStatusInvalid() throws Exception {
            when(reportService.handleReport(eq(1L), eq("BOGUS"), any()))
                    .thenThrow(new BadRequestException("无效的处理状态: BOGUS"));
            String body = "{\"status\":\"BOGUS\"}";
            mockMvc.perform(put("/api/admin/reports/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }
}
