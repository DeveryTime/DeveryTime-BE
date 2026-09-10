package com.dms.deverytime.domain.comment.dto.response;

import java.time.LocalDateTime;

public record CommentListResponse(
        Long id,
        Long userId,
        String authorName,
        String profileImageUrl,
        String content,
        LocalDateTime createdAt
) {
}