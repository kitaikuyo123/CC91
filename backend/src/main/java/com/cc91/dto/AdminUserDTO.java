package com.cc91.dto;

import java.time.LocalDateTime;

/**
 * 管理员用户信息 DTO
 */
public class AdminUserDTO {

    private Long id;
    private String username;
    private String email;
    private String role;
    private Boolean isLocked;
    private LocalDateTime createdAt;
    private LocalDateTime lockUntil;

    public AdminUserDTO() {}

    public AdminUserDTO(Long id, String username, String email, String role,
                        Boolean isLocked, LocalDateTime createdAt, LocalDateTime lockUntil) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.isLocked = isLocked;
        this.createdAt = createdAt;
        this.lockUntil = lockUntil;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Boolean getIsLocked() { return isLocked; }
    public void setIsLocked(Boolean isLocked) { this.isLocked = isLocked; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLockUntil() { return lockUntil; }
    public void setLockUntil(LocalDateTime lockUntil) { this.lockUntil = lockUntil; }
}
