package com.cc91.contentservice.controller;

import com.cc91.contentservice.dto.AnnouncementDTO;
import com.cc91.contentservice.dto.ApiResponse;
import com.cc91.contentservice.dto.CreateAnnouncementRequest;
import com.cc91.contentservice.dto.UpdateAnnouncementRequest;
import com.cc91.contentservice.exception.UnauthorizedException;
import com.cc91.contentservice.service.AnnouncementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告控制器
 * 公开端点 + 管理员端点
 */
@RestController
@RequestMapping("/api")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    // ==================== 公开端点 ====================

    /**
     * 获取所有公告（置顶优先，时间倒序）
     * GET /api/announcements
     */
    @GetMapping("/announcements")
    public ResponseEntity<List<AnnouncementDTO>> listAll() {
        List<AnnouncementDTO> announcements = announcementService.findAll();
        return ResponseEntity.ok(announcements);
    }

    /**
     * 获取公告详情
     * GET /api/announcements/{id}
     */
    @GetMapping("/announcements/{id}")
    public ResponseEntity<AnnouncementDTO> getDetail(@PathVariable Long id) {
        AnnouncementDTO announcement = announcementService.findById(id);
        return ResponseEntity.ok(announcement);
    }

    // ==================== 管理员端点 ====================

    /**
     * 创建公告
     * POST /api/admin/announcements
     */
    @PostMapping("/admin/announcements")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnnouncementDTO>> create(
            @Valid @RequestBody CreateAnnouncementRequest request
    ) {
        String username = getCurrentUsername();
        AnnouncementDTO announcement = announcementService.create(request, username);
        return ResponseEntity.ok(ApiResponse.success("公告创建成功", announcement));
    }

    /**
     * 更新公告
     * PUT /api/admin/announcements/{id}
     */
    @PutMapping("/admin/announcements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnnouncementDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAnnouncementRequest request
    ) {
        AnnouncementDTO announcement = announcementService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("公告更新成功", announcement));
    }

    /**
     * 删除公告
     * DELETE /api/admin/announcements/{id}
     */
    @DeleteMapping("/admin/announcements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("公告删除成功"));
    }

    /**
     * 从 Spring Security 上下文中获取当前登录用户名
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        throw new UnauthorizedException("用户未登录");
    }
}
