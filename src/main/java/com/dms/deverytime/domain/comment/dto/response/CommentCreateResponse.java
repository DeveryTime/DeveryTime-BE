package com.dms.deverytime.domain.comment.dto.response;

import java.time.LocalDateTime;

public record CommentCreateResponse(
        Long id,
        Long postId,
        Long userId,
        String content,
        LocalDateTime createdAt
) {
}