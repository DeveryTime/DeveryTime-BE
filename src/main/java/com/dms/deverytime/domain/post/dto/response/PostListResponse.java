package com.dms.deverytime.domain.post.dto.response;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class PostListResponse {

    //목록 조회 때 필요한 것들
    private Long id;
    private String title;
    private String categoryName;
    private LocalDateTime createdAt;

    public PostListResponse(Long id, String title, String categoryName, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.categoryName = categoryName;
        this.createdAt = createdAt;
    }
}
