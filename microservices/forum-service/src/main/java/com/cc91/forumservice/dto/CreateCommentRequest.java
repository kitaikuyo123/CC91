package com.cc91.forumservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建评论请求
 */
public class CreateCommentRequest {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 2000, message = "评论长度不能超过2000个字符")
    private String content;

    public CreateCommentRequest() {}

    public CreateCommentRequest(String content) {
        this.content = content;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
