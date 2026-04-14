package com.cc91.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建评论请求
 */
public class CreateCommentRequest {

    @NotBlank(message = "评论内容不能为空")
    private String content;

    public CreateCommentRequest() {}

    public CreateCommentRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
