package com.cc91.dto;

import java.time.LocalDateTime;

/**
 * 用户资料响应 DTO
 */
public class UserProfileDTO {

    private String username;
    private String email;
    private String avatarUrl;
    private String bio;
    private String location;
    private String website;
    private LocalDateTime createdAt;
    private String role;

    public UserProfileDTO() {}

    public UserProfileDTO(String username, String email, String avatarUrl, String bio,
                          String location, String website, LocalDateTime createdAt,
                          String role) {
        this.username = username;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.location = location;
        this.website = website;
        this.createdAt = createdAt;
        this.role = role;
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
