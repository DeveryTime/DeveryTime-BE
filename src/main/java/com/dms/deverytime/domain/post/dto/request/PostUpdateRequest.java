package com.dms.deverytime.domain.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class PostUpdateRequest {

    @NotBlank
    @Size (max = 255)
    private String title;

    @NotBlank
    private String content;

    public PostUpdateRequest() {
    }
}
