package com.dms.deverytime.domain.post.dto.request;

import lombok.Getter;

@Getter
public class PostUpdateRequest {

    private String title;
    private String content;

    public PostUpdateRequest() {
    }
}
