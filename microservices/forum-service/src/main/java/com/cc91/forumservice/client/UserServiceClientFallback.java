package com.cc91.forumservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Fallback for UserServiceClient when User Service is unavailable.
 * Returns safe defaults so forum operations can continue with degraded user info.
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
        // IMPORTANT: 必须返回 null，不能返回 id=null 的"虚假用户"。
        // 调用方（PostService / CommentService）依赖此契约：
        //   null => 用户不存在 => 抛 ResourceNotFoundException => 客户端拿到 404 + 清晰错误
        // 若返回 id=null 的占位对象，会让流程继续走到 INSERT，最终 MySQL
        // 因 author_id=null 拒绝写入，返回 400 'Column author_id cannot be null'。
        // （压测 Task 3 scenario-4 暴露的 P1 bug）
        logger.warn("User Service unavailable, returning null for username={} (caller should handle as 'user not found')", username);
        return null;
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
