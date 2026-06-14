package com.cc91.contentservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Fallback for UserServiceClient when User Service is unavailable.
 * Returns safe defaults so content operations can continue with degraded user info.
 */
@Component
public class UserServiceClientFallback implements UserServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceClientFallback.class);

    @Override
    public UserInfoDTO getUserById(Long id) {
        logger.warn("User Service unavailable, returning fallback for userId={}", id);
        return new UserInfoDTO(id, "未知用户", "USER", null);
    }

    @Override
    public UserInfoDTO getUserByUsername(String username) {
        logger.warn("User Service unavailable, returning fallback for username={}", username);
        return new UserInfoDTO(null, username, "USER", null);
    }

    @Override
    public List<UserInfoDTO> getUsersByIds(List<Long> ids) {
        logger.warn("User Service unavailable, returning fallback for {} user IDs", ids.size());
        return ids.stream()
                .map(id -> new UserInfoDTO(id, "未知用户", "USER", null))
                .toList();
    }

    @Override
    public Map<String, Boolean> isUserLocked(Long userId) {
        logger.warn("User Service unavailable, assuming user {} is not locked", userId);
        return Map.of("locked", false);
    }
}
