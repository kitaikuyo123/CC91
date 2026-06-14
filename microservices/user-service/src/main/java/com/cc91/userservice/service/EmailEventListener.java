package com.cc91.userservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EmailEventListener {

    private static final Logger logger = LoggerFactory.getLogger(EmailEventListener.class);

    private final EmailService emailService;

    public EmailEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmailEvent(EmailEvent event) {
        try {
            switch (event.getType()) {
                case VERIFICATION -> emailService.sendVerificationCode(event.getTo(), event.getCode(), event.getExpiresInSeconds());
                case PASSWORD_RESET -> emailService.sendPasswordResetCode(event.getTo(), event.getCode(), event.getExpiresInSeconds());
            }
        } catch (Exception e) {
            logger.error("Failed to handle email event: to={}, type={}, error={}", event.getTo(), event.getType(), e.getMessage(), e);
        }
    }
}
