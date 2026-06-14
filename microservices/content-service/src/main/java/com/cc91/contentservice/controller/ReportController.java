package com.cc91.contentservice.controller;

import com.cc91.contentservice.dto.ApiResponse;
import com.cc91.contentservice.dto.CreateReportRequest;
import com.cc91.contentservice.entity.Report;
import com.cc91.contentservice.exception.BadRequestException;
import com.cc91.contentservice.exception.UnauthorizedException;
import com.cc91.contentservice.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 举报控制器
 * 处理举报相关的请求
 */
@RestController
@RequestMapping("/api")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 用户提交举报
     * POST /api/reports
     */
    @PostMapping("/reports")
    public ResponseEntity<ApiResponse<Report>> createReport(
            @Valid @RequestBody CreateReportRequest request) {
        String username = getCurrentUsername();
        Report report = reportService.createReport(
                username,
                request.getContentId(),
                request.getContentType(),
                request.getReason(),
                request.getDescription()
        );
        return ResponseEntity.ok(ApiResponse.success("举报提交成功", report));
    }

    /**
     * 管理员获取举报列表
     * GET /api/admin/reports
     */
    @GetMapping("/admin/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Report>> getReports(
            @RequestParam(required = false) String status) {
        Page<Report> page = reportService.getReports(status, 0, 1000);
        return ResponseEntity.ok(page.getContent());
    }

    /**
     * 管理员处理举报
     * PUT /api/admin/reports/{id}
     */
    @PutMapping("/admin/reports/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Report>> handleReport(
            @PathVariable Long id,
            @RequestBody HandleReportRequest request) {
        String status = request.getStatus();
        if (status == null || status.trim().isEmpty()) {
            throw new BadRequestException("处理状态不能为空");
        }
        Report report = reportService.handleReport(id, status);
        return ResponseEntity.ok(ApiResponse.success("举报已处理", report));
    }

    public static class HandleReportRequest {
        private String status;
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        throw new UnauthorizedException("用户未登录");
    }
}
