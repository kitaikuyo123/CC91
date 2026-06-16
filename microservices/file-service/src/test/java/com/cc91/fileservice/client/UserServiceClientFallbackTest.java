package com.cc91.fileservice.client;

import com.cc91.fileservice.dto.UpdateAvatarRequest;
import com.cc91.fileservice.dto.UserInfoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserServiceClientFallback unit tests for file-service.
 *
 * Verifies the fallback returns safe defaults (never null, never throws) so that
 * file upload does not crash when User Service is unreachable. Resilience4j
 * invokes this fallback when the Feign call fails / circuit is open.
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
        @DisplayName("should return fallback UserInfoDTO with preserved username")
        void shouldReturnFallbackForUsername() {
            UserInfoDTO result = fallback.getUserByUsername("alice");
            assertNotNull(result);
            assertEquals("alice", result.getUsername());
            assertEquals("USER", result.getRole());
            assertNull(result.getId());
            assertNull(result.getAvatarUrl());
        }

        @Test
        @DisplayName("should handle null username gracefully (preserve null, not throw)")
        void shouldHandleNullUsername() {
            UserInfoDTO result = fallback.getUserByUsername(null);
            assertNotNull(result);
            assertNull(result.getUsername());
            assertEquals("USER", result.getRole());
        }
    }

    @Nested
    @DisplayName("updateAvatar")
    class UpdateAvatar {

        @Test
        @DisplayName("should return null and never throw when User Service is down")
        void shouldReturnNullWithoutThrowing() {
            UpdateAvatarRequest req = new UpdateAvatarRequest("/uploads/avatars/x.png");
            Void result = fallback.updateAvatar(42L, req);
            assertNull(result);
        }

        @Test
        @DisplayName("should not throw when called with null arguments")
        void shouldNotThrowOnNullArgs() {
            assertDoesNotThrow(() -> fallback.updateAvatar(null, null));
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
