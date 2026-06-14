package com.cc91.contentservice.dto;

import jakarta.validation.constraints.Size;

/**
 * 更新公告请求 DTO（支持部分更新）
 */
public class UpdateAnnouncementRequest {

    @Size(max = 200, message = "公告标题最多200个字符")
    private String title;

    private String content;

    private Boolean isPinned;

    public UpdateAnnouncementRequest() {}

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Boolean getIsPinned() { return isPinned; }
    public void setIsPinned(Boolean isPinned) { this.isPinned = isPinned; }
}
