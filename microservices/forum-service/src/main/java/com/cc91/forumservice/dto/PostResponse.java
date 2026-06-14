package com.cc91.forumservice.dto;

import java.time.LocalDateTime;

/**
 * 帖子响应 DTO
 */
public class PostResponse {

    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private String authorUsername;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer viewCount;
    private String status;
    private Long commentCount;
    private String authorAvatarUrl;
    private Long likeCount;
    private Boolean isLikedByCurrentUser;
    private Boolean isBookmarkedByCurrentUser;

    public PostResponse() {}

    public PostResponse(Long id, String title, String content, Long authorId,
                       String authorUsername, LocalDateTime createdAt,
                       LocalDateTime updatedAt, Integer viewCount, String status) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.viewCount = viewCount;
        this.status = status;
    }

    public PostResponse(Long id, String title, String content, Long authorId,
                       String authorUsername, Long categoryId, String categoryName,
                       LocalDateTime createdAt, LocalDateTime updatedAt, Integer viewCount,
                       String status, Long commentCount) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.viewCount = viewCount;
        this.status = status;
        this.commentCount = commentCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getCommentCount() { return commentCount; }
    public void setCommentCount(Long commentCount) { this.commentCount = commentCount; }

    public String getAuthorAvatarUrl() { return authorAvatarUrl; }
    public void setAuthorAvatarUrl(String authorAvatarUrl) { this.authorAvatarUrl = authorAvatarUrl; }

    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }

    public Boolean getIsLikedByCurrentUser() { return isLikedByCurrentUser; }
    public void setIsLikedByCurrentUser(Boolean isLikedByCurrentUser) { this.isLikedByCurrentUser = isLikedByCurrentUser; }

    public Boolean getIsBookmarkedByCurrentUser() { return isBookmarkedByCurrentUser; }
    public void setIsBookmarkedByCurrentUser(Boolean isBookmarkedByCurrentUser) { this.isBookmarkedByCurrentUser = isBookmarkedByCurrentUser; }
}
