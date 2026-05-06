package com.cc91.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新帖子状态请求 DTO
 */
public class UpdatePostStatusRequest {

    @NotBlank(message = "状态不能为空")
    private String status;

    public UpdatePostStatusRequest() {}

    public UpdatePostStatusRequest(String status) {
        this.status = status;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
