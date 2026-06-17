package com.cc91.forumservice.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserServiceClientFallback unit tests.
 *
 * 契约：
 *  - getUserById / getUsersByIds：使用 fallback 占位对象（保留 id），允许下游展示用
 *  - getUserByUsername：必须返回 null，让上游抛 ResourceNotFoundException
 *      （避免 id=null 占位导致 INSERT 失败 400，详见 issues.md scenario-4 修复）
 *  - isUserLocked：默认 false（保守地允许请求继续，由 JWT 状态保障安全）
 */
class UserServiceClientFallbackTest {

    private UserServiceClientFallback fallback;

    @BeforeEach
    void setUp() {
        fallback = new UserServiceClientFallback();
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("should return fallback UserInfoDTO with preserved id and '未知用户' username")
        void shouldReturnFallbackForId() {
            UserInfoDTO result = fallback.getUserById(42L);
            assertNotNull(result);
            assertEquals(42L, result.getId());
            assertEquals("未知用户", result.getUsername());
            assertEquals("USER", result.getRole());
            assertNull(result.getAvatarUrl());
        }

        @Test
        @DisplayName("should handle null id gracefully")
        void shouldHandleNullId() {
            UserInfoDTO result = fallback.getUserById(null);
            assertNotNull(result);
            assertNull(result.getId());
        }
    }

    @Nested
    @DisplayName("getUserByUsername")
    class GetUserByUsername {

        /**
         * 契约：User Service 不可用时必须返回 null，而不是 id=null 的占位对象。
         *
         * 否则调用方（PostService/CommentService）已有的
         *   if (user == null) throw new ResourceNotFoundException("用户不存在")
         * 校验会失效，导致流程继续走到 INSERT，最终 MySQL 拒绝 author_id=null
         * 报 400 'Column author_id cannot be null'。
         *
         * 详见 docs/issues.md scenario-4 修复记录。
         */
        @Test
        @DisplayName("should return null so callers can throw ResourceNotFoundException (not a ghost user with null id)")
        void shouldReturnNullForUsername() {
            UserInfoDTO result = fallback.getUserByUsername("alice");
            assertNull(result,
                    "fallback must return null when user-service is unavailable; "
                            + "returning an id=null placeholder causes 'author_id cannot be null' at INSERT");
        }

        @Test
        @DisplayName("should also return null for null username")
        void shouldReturnNullForNullUsername() {
            UserInfoDTO result = fallback.getUserByUsername(null);
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("getUsersByIds")
    class GetUsersByIds {

        @Test
        @DisplayName("should return fallback list preserving order and ids")
        void shouldReturnFallbackList() {
            List<UserInfoDTO> result = fallback.getUsersByIds(List.of(1L, 2L, 3L));
            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals(1L, result.get(0).getId());
            assertEquals(2L, result.get(1).getId());
            assertEquals(3L, result.get(2).getId());
            result.forEach(u -> assertEquals("未知用户", u.getUsername()));
        }

        @Test
        @DisplayName("should return empty list for empty input")
        void shouldReturnEmptyForEmptyInput() {
            List<UserInfoDTO> result = fallback.getUsersByIds(List.of());
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should not throw when input list contains nulls")
        void shouldNotThrowOnNulls() {
            List<UserInfoDTO> result = fallback.getUsersByIds(java.util.Arrays.asList(1L, null, 3L));
            assertNotNull(result);
            assertEquals(3, result.size());
        }
    }

    @Nested
    @DisplayName("isUserLocked")
    class IsUserLocked {

        @Test
        @DisplayName("should always return locked=false")
        void shouldReturnNotLocked() {
            Map<String, Boolean> result = fallback.isUserLocked(42L);
            assertNotNull(result);
            assertEquals(Boolean.FALSE, result.get("locked"));
        }

        @Test
        @DisplayName("should never throw, even for null id")
        void shouldNeverThrow() {
            Map<String, Boolean> result = fallback.isUserLocked(null);
            assertNotNull(result);
            assertEquals(Boolean.FALSE, result.get("locked"));
        }
    }
}
