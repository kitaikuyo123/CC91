package com.cc91.forumservice.dto;

import java.time.LocalDateTime;

/**
 * 版块分类响应 DTO
 */
public class CategoryDTO {

    private Long id;
    private String name;
    private String description;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private long postCount;
    private long todayPostCount;

    public CategoryDTO() {}

    public CategoryDTO(Long id, String name, String description, Integer sortOrder,
                       LocalDateTime createdAt, long postCount, long todayPostCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.postCount = postCount;
        this.todayPostCount = todayPostCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public long getPostCount() { return postCount; }
    public void setPostCount(long postCount) { this.postCount = postCount; }

    public long getTodayPostCount() { return todayPostCount; }
    public void setTodayPostCount(long todayPostCount) { this.todayPostCount = todayPostCount; }
}
