package com.cc91.notificationservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserServiceClientFallback implements UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClientFallback.class);

    @Override
    public UserInfoDTO getUserByUsername(String username) {
        log.warn("Fallback: getUserByUsername({}) — user-service unavailable", username);
        return new UserInfoDTO(-1L, username, "UNKNOWN", null);
    }

    @Override
    public UserInfoDTO getUserById(Long id) {
        log.warn("Fallback: getUserById({}) — user-service unavailable", id);
        return new UserInfoDTO(id, "unknown", "UNKNOWN", null);
    }

    @Override
    public Map<String, Boolean> isUserLocked(Long userId) {
        log.warn("Fallback: isUserLocked({}) — user-service unavailable, assuming unlocked", userId);
        return Map.of("locked", false);
    }
}
