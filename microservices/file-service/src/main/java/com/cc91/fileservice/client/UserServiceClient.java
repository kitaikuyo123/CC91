package com.cc91.fileservice.client;

import com.cc91.fileservice.dto.UpdateAvatarRequest;
import com.cc91.fileservice.dto.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign client for calling User Service internal APIs.
 */
@FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {

    @GetMapping("/api/users/internal/username/{username}")
    UserInfoDTO getUserByUsername(@PathVariable("username") String username);

    @PutMapping("/api/users/internal/{id}/avatar")
    Void updateAvatar(@PathVariable("id") Long id, @RequestBody UpdateAvatarRequest request);

    @GetMapping("/api/users/internal/{userId}/locked")
    Map<String, Boolean> isUserLocked(@PathVariable("userId") Long userId);
}
