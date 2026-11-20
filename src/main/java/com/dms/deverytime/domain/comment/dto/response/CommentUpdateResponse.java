package com.dms.deverytime.domain.comment.dto.response;

import com.dms.deverytime.domain.comment.entity.Comment;

import java.time.LocalDateTime;

public record CommentUpdateResponse(
        Long id,
        String content,
        LocalDateTime updatedAt
) {
    public static CommentUpdateResponse from(Comment comment){
        return new CommentUpdateResponse(
                comment.getId(), comment.getContent(), comment.getUpdatedAt()
        );
    }
}