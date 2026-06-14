package com.cc91.contentservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建举报请求
 */
public class CreateReportRequest {

    @NotNull(message = "目标ID不能为空")
    private Long contentId;

    @NotBlank(message = "目标类型不能为空")
    @jakarta.validation.constraints.Pattern(regexp = "POST|COMMENT", message = "目标类型必须是 POST 或 COMMENT")
    private String contentType;

    @NotBlank(message = "举报原因不能为空")
    @Size(max = 500, message = "举报原因不能超过500个字符")
    private String reason;

    @Size(max = 500, message = "补充描述不能超过500个字符")
    private String description;

    public CreateReportRequest() {}

    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
