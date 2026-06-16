package com.cc91.contentservice.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * NotificationServiceClientFallback unit tests.
 * The fallback's contract: never throw, swallow the failure (degrade silently).
 */
class NotificationServiceClientFallbackTest {

    private NotificationServiceClientFallback fallback;

    @BeforeEach
    void setUp() {
        fallback = new NotificationServiceClientFallback();
    }

    @Test
    @DisplayName("createNotification should complete silently for valid request")
    void shouldCompleteSilently() {
        CreateNotificationRequest req = new CreateNotificationRequest(
                1L, "ANNOUNCEMENT", "title", "content", 10L);
        assertDoesNotThrow(() -> fallback.createNotification(req));
    }

    @Test
    @DisplayName("createNotification should complete silently for partial fields")
    void shouldCompleteForPartialFields() {
        CreateNotificationRequest req = new CreateNotificationRequest(
                99L, "SYSTEM", "title", null, null);
        assertDoesNotThrow(() -> fallback.createNotification(req));
    }
}
