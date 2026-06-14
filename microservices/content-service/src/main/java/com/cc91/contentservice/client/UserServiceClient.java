package com.cc91.contentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {

    @GetMapping("/api/users/internal/{id}")
    UserInfoDTO getUserById(@PathVariable("id") Long id);

    @GetMapping("/api/users/internal/username/{username}")
    UserInfoDTO getUserByUsername(@PathVariable("username") String username);

    @PostMapping("/api/users/internal/batch")
    List<UserInfoDTO> getUsersByIds(@RequestBody List<Long> ids);

    @GetMapping("/api/users/internal/{userId}/locked")
    Map<String, Boolean> isUserLocked(@PathVariable("userId") Long userId);
}
