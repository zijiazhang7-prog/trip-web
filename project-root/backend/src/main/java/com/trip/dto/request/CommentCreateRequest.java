package com.trip.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 评论发布请求。
 */
public class CommentCreateRequest {

    @NotBlank
    @Size(max = 500)
    private String contentText;

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }
}
