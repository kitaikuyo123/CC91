package com.cc91.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建通知请求 DTO (用于内部服务调用)
 */
public class CreateNotificationRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "通知类型不能为空")
    private String type;

    @NotBlank(message = "通知标题不能为空")
    private String title;

    private String content;

    private Long relatedId;

    public CreateNotificationRequest() {}

    public CreateNotificationRequest(Long userId, String type, String title, String content, Long relatedId) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.relatedId = relatedId;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getRelatedId() { return relatedId; }
    public void setRelatedId(Long relatedId) { this.relatedId = relatedId; }
}
