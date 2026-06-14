package com.cc91.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateUserRoleRequest {
    @NotBlank
    @Pattern(regexp = "USER|ADMIN", message = "角色必须是 USER 或 ADMIN")
    private String role;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
