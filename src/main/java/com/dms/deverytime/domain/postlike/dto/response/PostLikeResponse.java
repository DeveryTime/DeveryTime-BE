package com.dms.deverytime.domain.postlike.dto.response;

public record PostLikeResponse(
        Long postId,
        Long userId,
        boolean liked
) {
}