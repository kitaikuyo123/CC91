package com.cc91.forumservice.dto;

import jakarta.validation.constraints.Size;

/**
 * 更新帖子请求 DTO
 */
public class UpdatePostRequest {

    @Size(min = 1, max = 200, message = "标题长度应在1-200个字符之间")
    private String title;

    @Size(max = 50000, message = "内容长度不能超过50000个字符")
    private String content;

    private Long categoryId;

    private String status;

    public UpdatePostRequest() {}

    public UpdatePostRequest(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
