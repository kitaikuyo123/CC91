package com.cc91.dto;

import java.time.LocalDateTime;

/**
 * 当前用户评论列表响应 DTO（用于 Dashboard "我的评论"）
 */
public class UserCommentResponse {

    private Long id;
    private Long postId;
    private String postTitle;
    private String content;
    private Long parentId;
    private LocalDateTime createdAt;
    private String status;

    public UserCommentResponse() {}

    public UserCommentResponse(Long id, Long postId, String postTitle, String content, Long parentId, LocalDateTime createdAt, String status) {
        this.id = id;
        this.postId = postId;
        this.postTitle = postTitle;
        this.content = content;
        this.parentId = parentId;
        this.createdAt = createdAt;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public String getPostTitle() { return postTitle; }
    public void setPostTitle(String postTitle) { this.postTitle = postTitle; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
