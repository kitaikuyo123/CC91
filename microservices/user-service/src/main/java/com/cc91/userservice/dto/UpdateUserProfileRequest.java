package com.cc91.userservice.dto;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

/**
 * 更新用户资料请求 DTO
 */
public class UpdateUserProfileRequest {

    @Size(max = 500, message = "头像URL不能超过500字符")
    private String avatarUrl;

    @Size(max = 500, message = "个人签名不能超过500字符")
    private String bio;

    @Size(max = 100, message = "所在地不能超过100字符")
    private String location;

    @Size(max = 200, message = "网站地址不能超过200字符")
    @URL(message = "网站地址格式不正确")
    private String website;

    // Getters and Setters
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
}
