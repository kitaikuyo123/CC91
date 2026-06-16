package com.cc91.notificationservice.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserServiceClientFallback unit tests for notification-service.
 * Verifies safe defaults are returned (never null, never throws).
 *
 * Note: notification-service's fallback is intentionally different from
 * content-service: it returns id=-1 for getUserByUsername (sentinel) and
 * "unknown" username for getUserById.
 */
class UserServiceClientFallbackTest {

    private UserServiceClientFallback fallback;

    @BeforeEach
    void setUp() {
        fallback = new UserServiceClientFallback();
    }

    @Nested
    @DisplayName("getUserByUsername")
    class GetUserByUsername {

        @Test
        @DisplayName("should return fallback with preserved username, sentinel id=-1, role=UNKNOWN")
        void shouldReturnFallbackForUsername() {
            UserInfoDTO result = fallback.getUserByUsername("alice");
            assertNotNull(result);
            assertEquals("alice", result.getUsername());
            assertEquals(-1L, result.getId());
            assertEquals("UNKNOWN", result.getRole());
            assertNull(result.getAvatarUrl());
        }

        @Test
        @DisplayName("should handle null username gracefully")
        void shouldHandleNullUsername() {
            UserInfoDTO result = fallback.getUserByUsername(null);
            assertNotNull(result);
            assertNull(result.getUsername());
            assertEquals(-1L, result.getId());
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("should return fallback with preserved id, 'unknown' username, role=UNKNOWN")
        void shouldReturnFallbackForId() {
            UserInfoDTO result = fallback.getUserById(42L);
            assertNotNull(result);
            assertEquals(42L, result.getId());
            assertEquals("unknown", result.getUsername());
            assertEquals("UNKNOWN", result.getRole());
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
