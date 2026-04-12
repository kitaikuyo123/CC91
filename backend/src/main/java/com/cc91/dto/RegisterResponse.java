package com.cc91.dto;

/**
 * 用户注册响应 DTO
 */
public class RegisterResponse {

    private String message;
    private Integer expiresIn;

    public RegisterResponse() {}

    public RegisterResponse(String message, Integer expiresIn) {
        this.message = message;
        this.expiresIn = expiresIn;
    }

    // Getters and Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Integer getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Integer expiresIn) { this.expiresIn = expiresIn; }
}
