package com.cc91.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 更新用户角色请求 DTO
 */
public class UpdateUserRoleRequest {

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "^(USER|ADMIN)$", message = "无效的角色值，仅支持 USER 或 ADMIN")
    private String role;

    public UpdateUserRoleRequest() {}

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
