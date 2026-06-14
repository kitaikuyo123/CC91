package com.cc91.forumservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "notification-service", fallback = NotificationServiceClientFallback.class)
public interface NotificationServiceClient {

    @PostMapping("/api/notifications/internal")
    void createNotification(@RequestBody CreateNotificationRequest request);
}
