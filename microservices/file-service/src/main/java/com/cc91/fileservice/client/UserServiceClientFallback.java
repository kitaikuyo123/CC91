package com.cc91.fileservice.client;

import com.cc91.fileservice.dto.UpdateAvatarRequest;
import com.cc91.fileservice.dto.UserInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Fallback for UserServiceClient when User Service is unavailable.
 * Returns safe defaults so file operations can report meaningful errors.
 */
@Component
public class UserServiceClientFallback implements UserServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceClientFallback.class);

    @Override
    public UserInfoDTO getUserByUsername(String username) {
        logger.warn("User Service unavailable, returning fallback for username={}", username);
        return new UserInfoDTO(null, username, "USER", null);
    }

    @Override
    public Void updateAvatar(Long id, UpdateAvatarRequest request) {
        logger.warn("User Service unavailable, cannot update avatar for userId={}", id);
        return null;
    }

    @Override
    public Map<String, Boolean> isUserLocked(Long userId) {
        logger.warn("User Service unavailable, assuming user {} is not locked", userId);
        return Map.of("locked", false);
    }
}
