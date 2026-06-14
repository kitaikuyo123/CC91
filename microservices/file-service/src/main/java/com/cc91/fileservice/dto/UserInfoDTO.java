package com.cc91.fileservice.dto;

/**
 * Lightweight user info DTO received from User Service via Feign.
 */
public class UserInfoDTO {

    private Long id;
    private String username;
    private String role;
    private String avatarUrl;

    public UserInfoDTO() {}

    public UserInfoDTO(Long id, String username, String role, String avatarUrl) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.avatarUrl = avatarUrl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
