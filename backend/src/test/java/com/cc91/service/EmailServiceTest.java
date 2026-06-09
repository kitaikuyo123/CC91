package com.cc91.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * EmailService business logic tests
 * Covers console-log-only mode, actual mail sending, MailException handling, and email content correctness
 */
@SpringBootTest
@ActiveProfiles("test")
class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @MockBean
    private JavaMailSender mailSender;

    // ==================== consoleLogOnly=true ====================

    @Test
    void sendVerificationCode_ConsoleLogOnly_DoeNotCallMailSender() {
        // Arrange: consoleLogOnly=true (default in test profile)
        ReflectionTestUtils.setField(emailService, "consoleLogOnly", true);

        // Act
        emailService.sendVerificationCode("test@example.com", "123456", 600);

        // Assert: mailSender never called
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendPasswordResetCode_ConsoleLogOnly_DoesNotCallMailSender() {
        // Arrange
        ReflectionTestUtils.setField(emailService, "consoleLogOnly", true);

        // Act
        emailService.sendPasswordResetCode("test@example.com", "654321", 300);

        // Assert: mailSender never called
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // ==================== consoleLogOnly=false ====================

    @Test
    void sendVerificationCode_SendsCorrectEmail() {
        // Arrange
        ReflectionTestUtils.setField(emailService, "consoleLogOnly", false);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        emailService.sendVerificationCode("user@cc91.com", "888888", 600);

        // Assert: mailSender called once
        verify(mailSender, times(1)).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertEquals("test", msg.getFrom());  // spring.mail.username in test config
        assertEquals("user@cc91.com", msg.getTo()[0]);
        assertEquals("CC91 论坛 - 邮箱验证码", msg.getSubject());
        String text = msg.getText();
        assertTrue(text.contains("888888"));
        assertTrue(text.contains("10 分钟后过期"));  // 600s / 60 = 10 min
    }

    @Test
    void sendPasswordResetCode_SendsCorrectEmail() {
        // Arrange
        ReflectionTestUtils.setField(emailService, "consoleLogOnly", false);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        emailService.sendPasswordResetCode("user@cc91.com", "999999", 300);

        // Assert: mailSender called once
        verify(mailSender, times(1)).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertEquals("test", msg.getFrom());  // spring.mail.username in test config
        assertEquals("user@cc91.com", msg.getTo()[0]);
        assertEquals("CC91 论坛 - 密码重置验证码", msg.getSubject());
        String text = msg.getText();
        assertTrue(text.contains("999999"));
        assertTrue(text.contains("5 分钟后过期"));  // 300s / 60 = 5 min
    }

    // ==================== MailException 处理 ====================

    @Test
    void sendVerificationCode_MailException_DoesNotThrow() {
        // Arrange
        ReflectionTestUtils.setField(emailService, "consoleLogOnly", false);
        doThrow(new MailSendException("SMTP connection failed"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert: MailException caught, not propagated
        assertDoesNotThrow(() ->
                emailService.sendVerificationCode("test@example.com", "123456", 600));

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendPasswordResetCode_MailException_DoesNotThrow() {
        // Arrange
        ReflectionTestUtils.setField(emailService, "consoleLogOnly", false);
        doThrow(new MailSendException("SMTP timeout"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert: MailException caught, not propagated
        assertDoesNotThrow(() ->
                emailService.sendPasswordResetCode("test@example.com", "654321", 300));

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
