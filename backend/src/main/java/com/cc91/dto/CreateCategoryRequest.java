package com.cc91.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建版块分类请求 DTO
 */
public class CreateCategoryRequest {

    @NotBlank(message = "版块名称不能为空")
    @Size(max = 50, message = "版块名称最多50个字符")
    private String name;

    @Size(max = 500, message = "版块描述最多500个字符")
    private String description;

    private Integer sortOrder;

    public CreateCategoryRequest() {}

    public CreateCategoryRequest(String name, String description, Integer sortOrder) {
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
