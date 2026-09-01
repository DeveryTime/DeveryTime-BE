package com.dms.deverytime.domain.post.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostRequest {

    private Long categoryId;
    private String title;
    private String content;
    private String status;

    // 기본 생성자
    public PostRequest() {
    }
}
