package com.dms.deverytime.domain.comment.dto.response;

import java.time.LocalDateTime;

public record CommentUpdateResponse(
        Long id,
        String content,
        LocalDateTime updatedAt
) {
}