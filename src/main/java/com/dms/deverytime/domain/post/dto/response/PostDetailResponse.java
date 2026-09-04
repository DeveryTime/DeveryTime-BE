package com.dms.deverytime.domain.post.dto.response;

import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class PostDetailResponse {

    // 게시글 상세 조회시 필요 정보
    private Long id;
    private String title;
    private String content;
    private String status;
    private int viewCount;
    private String writerNickname;
    private String writerProfileImageUrl;
    private String categoryName;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PostDetailResponse(Long id, String title, String content, String status, int viewCount,
                              String writerNickname, String writerProfileImageUrl, String categoryName, List<String> imageUrls,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.status = status;
        this.viewCount = viewCount;
        this.writerNickname = writerNickname;
        this.writerProfileImageUrl = writerProfileImageUrl;
        this.categoryName = categoryName;
        this.imageUrls = imageUrls;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
