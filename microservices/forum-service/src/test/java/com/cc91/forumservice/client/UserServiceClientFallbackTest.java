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
 * Verifies safe defaults are returned (never null, never throws).
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

        @Test
        @DisplayName("should return fallback with preserved username and null id")
        void shouldReturnFallbackForUsername() {
            UserInfoDTO result = fallback.getUserByUsername("alice");
            assertNotNull(result);
            assertEquals("alice", result.getUsername());
            assertNull(result.getId());
            assertEquals("USER", result.getRole());
        }

        @Test
        @DisplayName("should handle null username gracefully")
        void shouldHandleNullUsername() {
            UserInfoDTO result = fallback.getUserByUsername(null);
            assertNotNull(result);
            assertNull(result.getUsername());
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
