package com.cc91.dto;

import jakarta.validation.constraints.Size;

/**
 * 更新帖子请求 DTO
 */
public class UpdatePostRequest {

    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    private String content;

    public UpdatePostRequest() {}

    public UpdatePostRequest(String title, String content) {
        this.title = title;
        this.content = content;
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
