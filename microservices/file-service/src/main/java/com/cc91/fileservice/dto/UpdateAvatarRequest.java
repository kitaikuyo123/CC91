package com.cc91.fileservice.dto;

/**
 * Request DTO for updating user avatar URL via User Service.
 */
public class UpdateAvatarRequest {

    private String avatarUrl;

    public UpdateAvatarRequest() {}

    public UpdateAvatarRequest(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
