package com.dms.deverytime.domain.post.dto.response;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class PostListResponse {

    //목록 조회 때 필요한 것들
    private Long id;
    private String title;
    private Long writerId;
    private String categoryName;
    private LocalDateTime createdAt;
    private int likeCount;

    public PostListResponse(Long id, String title, Long writerId, String categoryName, LocalDateTime createdAt, int likeCount) {
        this.id = id;
        this.title = title;
        this.writerId = writerId;
        this.categoryName = categoryName;
        this.createdAt = createdAt;
        this.likeCount = likeCount;
    }
}
