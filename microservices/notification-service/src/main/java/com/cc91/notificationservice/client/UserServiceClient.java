package com.cc91.notificationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign client for calling User Service
 */
@FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {

    @GetMapping("/api/users/internal/username/{username}")
    UserInfoDTO getUserByUsername(@PathVariable String username);

    @GetMapping("/api/users/internal/{id}")
    UserInfoDTO getUserById(@PathVariable Long id);

    @GetMapping("/api/users/internal/{userId}/locked")
    Map<String, Boolean> isUserLocked(@PathVariable("userId") Long userId);
}
