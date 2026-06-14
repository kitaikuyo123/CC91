package com.cc91.userservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.console-log-only:true}")
    private boolean consoleLogOnly;

    @Value("${spring.mail.username:noreply@cc91.com}")
    private String fromAddress;

    @Autowired
    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationCode(String to, String code, int expiresInSeconds) {
        String subject = "CC91 论坛 - 邮箱验证码";
        send(to, subject, code, expiresInSeconds);
    }

    public void sendPasswordResetCode(String to, String code, int expiresInSeconds) {
        String subject = "CC91 论坛 - 密码重置验证码";
        send(to, subject, code, expiresInSeconds);
    }

    private void send(String to, String subject, String code, int expiresInSeconds) {
        if (consoleLogOnly || mailSender == null) {
            logger.info("=== EMAIL (console-only) ===\n  To: {}\n  Code: {}\n  ExpiresIn: {}s\n  Subject: {}", to, code, expiresInSeconds, subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(buildBody(code, expiresInSeconds));

            mailSender.send(message);
            logger.info("Email sent to {}: {} (code={}, expiresIn={}s)", to, subject, code, expiresInSeconds);
        } catch (MailException e) {
            logger.error("Failed to send email to {}: code={}, expiresIn={}s, error={}", to, code, expiresInSeconds, e.getMessage(), e);
        }
    }

    private String buildBody(String code, int expiresInSeconds) {
        return "您的验证码是：" + code + "\n\n"
                + "验证码将在 " + expiresInSeconds / 60 + " 分钟后过期。\n"
                + "如非本人操作，请忽略此邮件。\n\n"
                + "— CC91 论坛团队";
    }
}
