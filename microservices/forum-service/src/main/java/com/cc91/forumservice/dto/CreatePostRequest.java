package com.cc91.forumservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建帖子请求 DTO
 */
public class CreatePostRequest {

    @NotBlank(message = "标题不能为空")
    @Size(min = 1, max = 200, message = "标题长度应在1-200个字符之间")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size(max = 50000, message = "内容长度不能超过50000个字符")
    private String content;

    @NotNull(message = "版块不能为空")
    private Long categoryId;

    @Pattern(regexp = "^(PUBLISHED|DRAFT)$", message = "状态必须是 PUBLISHED 或 DRAFT")
    private String status;

    public CreatePostRequest() {}

    public CreatePostRequest(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public CreatePostRequest(String title, String content, Long categoryId) {
        this.title = title;
        this.content = content;
        this.categoryId = categoryId;
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
