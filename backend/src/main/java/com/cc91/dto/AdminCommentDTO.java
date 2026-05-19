package com.cc91.dto;

import java.time.LocalDateTime;

/**
 * 管理员评论视图 DTO
 */
public class AdminCommentDTO {

    private Long id;
    private Long postId;
    private String postTitle;
    private Long authorId;
    private String authorUsername;
    private String content;
    private Long parentId;
    private String status;
    private LocalDateTime createdAt;

    public AdminCommentDTO() {}

    public AdminCommentDTO(Long id, Long postId, String postTitle, Long authorId,
                           String authorUsername, String content, Long parentId,
                           String status, LocalDateTime createdAt) {
        this.id = id;
        this.postId = postId;
        this.postTitle = postTitle;
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.content = content;
        this.parentId = parentId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public String getPostTitle() { return postTitle; }
    public void setPostTitle(String postTitle) { this.postTitle = postTitle; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
