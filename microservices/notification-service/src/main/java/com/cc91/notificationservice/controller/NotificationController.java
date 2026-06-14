package com.cc91.notificationservice.controller;

import com.cc91.notificationservice.client.UserInfoDTO;
import com.cc91.notificationservice.client.UserServiceClient;
import com.cc91.notificationservice.dto.ApiResponse;
import com.cc91.notificationservice.dto.CreateNotificationRequest;
import com.cc91.notificationservice.dto.NotificationDTO;
import com.cc91.notificationservice.exception.UnauthorizedException;
import com.cc91.notificationservice.security.JwtUtil;
import com.cc91.notificationservice.service.NotificationServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知控制器
 * 处理通知相关的请求
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationServiceImpl notificationService;
    private final UserServiceClient userServiceClient;
    private final JwtUtil jwtUtil;

    public NotificationController(NotificationServiceImpl notificationService, UserServiceClient userServiceClient, JwtUtil jwtUtil) {
        this.notificationService = notificationService;
        this.userServiceClient = userServiceClient;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 获取当前用户的通知列表
     * GET /api/notifications?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        List<NotificationDTO> notifications = notificationService.getNotifications(userId, page, size);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 获取当前用户的未读通知数量
     * GET /api/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * 标记通知为已读
     * PUT /api/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.success("通知已标记为已读"));
    }

    /**
     * 标记所有通知为已读
     * PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("所有通知已标记为已读"));
    }

    /**
     * 删除通知
     * DELETE /api/notifications/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.ok(ApiResponse.success("通知已删除"));
    }

    /**
     * 内部API：创建通知（供其他微服务调用）
     * POST /api/notifications/internal
     */
    @PostMapping("/internal")
    public ResponseEntity<ApiResponse<Void>> createNotificationInternal(
            @Valid @RequestBody CreateNotificationRequest request
    ) {
        notificationService.createNotification(
                request.getUserId(),
                request.getType(),
                request.getTitle(),
                request.getContent(),
                request.getRelatedId()
        );
        return ResponseEntity.ok(ApiResponse.success("通知创建成功"));
    }

    /**
     * Extract userId from JWT claims — no Feign call needed.
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            String token = bearer.substring(7);
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId != null) {
                return userId;
            }
        }
        throw new UnauthorizedException("用户未登录");
    }
}
