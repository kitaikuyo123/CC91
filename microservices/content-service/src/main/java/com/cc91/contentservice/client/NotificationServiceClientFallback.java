package com.cc91.contentservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback for NotificationServiceClient when Notification Service is unavailable.
 * Logs the failure but does not block announcement creation.
 */
@Component
public class NotificationServiceClientFallback implements NotificationServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceClientFallback.class);

    @Override
    public void createNotification(CreateNotificationRequest request) {
        logger.warn("Notification Service unavailable, dropping notification for userId={}", request.getUserId());
    }
}
