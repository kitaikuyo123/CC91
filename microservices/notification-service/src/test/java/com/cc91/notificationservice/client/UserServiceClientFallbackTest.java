package com.cc91.notificationservice.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserServiceClientFallback unit tests for notification-service.
 *
 * getUserByUsername returns null when User Service is down — callers must treat
 * the user as not found (an earlier version returned a sentinel id=-1, which
 * risked routing notifications to a non-existent user).
 * Other methods return safe defaults as before.
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
        @DisplayName("should return null when User Service is down (never synthesize a sentinel user)")
        void shouldReturnNullForUsername() {
            UserInfoDTO result = fallback.getUserByUsername("alice");
            assertNull(result);
        }

        @Test
        @DisplayName("should return null for null username as well")
        void shouldReturnNullForNullUsername() {
            UserInfoDTO result = fallback.getUserByUsername(null);
            assertNull(result);
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
