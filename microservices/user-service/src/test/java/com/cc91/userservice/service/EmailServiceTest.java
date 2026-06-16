package com.cc91.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class EmailServiceTest {

    private EmailService emailService;
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        emailService = new EmailService(mailSender);
        // console-log-only mode (per application-test.yml default)
        ReflectionTestUtils.setField(emailService, "consoleLogOnly", true);
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@cc91.com");
    }

    @Test
    @DisplayName("sendVerificationCode should not throw and not call mailSender in console-only mode")
    void sendVerificationCodeConsoleOnly() {
        assertDoesNotThrow(() -> emailService.sendVerificationCode("a@b.com", "123456", 600));
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("sendPasswordResetCode should not throw in console-only mode")
    void sendPasswordResetCodeConsoleOnly() {
        assertDoesNotThrow(() -> emailService.sendPasswordResetCode("a@b.com", "654321", 600));
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("should not throw when mailSender is null and console-only is true")
    void shouldNotThrowWhenMailSenderNull() {
        EmailService nullSenderService = new EmailService(null);
        ReflectionTestUtils.setField(nullSenderService, "consoleLogOnly", true);
        ReflectionTestUtils.setField(nullSenderService, "fromAddress", "noreply@cc91.com");
        assertDoesNotThrow(() -> nullSenderService.sendVerificationCode("a@b.com", "1", 600));
    }
}
