package com.dms.deverytime.domain.comment.dto.response;

import com.dms.deverytime.domain.comment.entity.Comment;

import java.time.LocalDateTime;

public record CommentListResponse(
        Long id,
        Long userId,
        String authorName,
        String profileImageUrl,
        String content,
        LocalDateTime createdAt
) {
    public static CommentListResponse from(Comment comment) {
        return new CommentListResponse(
                comment.getId(),
                comment.getUser().getId(),
                comment.getUser().getName(),
                comment.getUser().getProfileImageUrl(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}