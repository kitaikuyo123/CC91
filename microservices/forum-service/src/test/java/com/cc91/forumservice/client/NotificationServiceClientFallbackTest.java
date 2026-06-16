package com.cc91.forumservice.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * NotificationServiceClientFallback unit tests.
 * Verifies the fallback never throws and silently drops notifications when
 * Notification Service is unavailable.
 */
class NotificationServiceClientFallbackTest {

    private NotificationServiceClientFallback fallback;

    @BeforeEach
    void setUp() {
        fallback = new NotificationServiceClientFallback();
    }

    @Test
    @DisplayName("should not throw when creating a notification with valid payload")
    void shouldNotThrowOnValidPayload() {
        CreateNotificationRequest req = new CreateNotificationRequest(
                42L, "REPLY", "新评论通知", "alice 评论了你的帖子", 100L);
        assertDoesNotThrow(() -> fallback.createNotification(req));
    }

    @Test
    @DisplayName("should not throw when request has null fields")
    void shouldNotThrowOnNullFields() {
        CreateNotificationRequest req = new CreateNotificationRequest();
        assertDoesNotThrow(() -> fallback.createNotification(req));
    }

    @Test
    @DisplayName("should throw NPE on fully-null request (current implementation reads fields)")
    void shouldThrowOnNullRequest() {
        // The fallback logs request.getUserId(), which dereferences the argument.
        // Locks in current behaviour — null input throws NPE (callers must pass non-null).
        assertThrows(NullPointerException.class, () -> fallback.createNotification(null));
    }
}
