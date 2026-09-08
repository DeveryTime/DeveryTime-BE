package com.dms.deverytime.domain.postlike.dto.response;

import lombok.Getter;

@Getter
public class PostLikeResponse {
    private Long postId;
    private Long userId;
    private boolean liked;

    public PostLikeResponse(Long postId, Long userId, boolean liked) {
        this.postId = postId;
        this.userId = userId;
        this.liked = liked;
    }
}
