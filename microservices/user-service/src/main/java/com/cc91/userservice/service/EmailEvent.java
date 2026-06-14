package com.cc91.userservice.service;

import org.springframework.context.ApplicationEvent;

public class EmailEvent extends ApplicationEvent {

    private final String to;
    private final String code;
    private final int expiresInSeconds;
    private final EmailType type;

    public enum EmailType {
        VERIFICATION, PASSWORD_RESET
    }

    public EmailEvent(Object source, String to, String code, int expiresInSeconds, EmailType type) {
        super(source);
        this.to = to;
        this.code = code;
        this.expiresInSeconds = expiresInSeconds;
        this.type = type;
    }

    public String getTo() { return to; }
    public String getCode() { return code; }
    public int getExpiresInSeconds() { return expiresInSeconds; }
    public EmailType getType() { return type; }
}
