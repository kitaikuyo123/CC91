package com.cc91.dto;

/**
 * 通用 API 响应 DTO
 */
public class ApiResponse<T> {

    private String message;
    private T data;

    public ApiResponse() {}

    public ApiResponse(String message) {
        this.message = message;
    }

    public ApiResponse(String message, T data) {
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(message);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(message, data);
    }

    // Getters and Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
