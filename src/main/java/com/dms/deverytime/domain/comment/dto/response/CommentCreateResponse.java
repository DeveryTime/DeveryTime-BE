package com.dms.deverytime.domain.comment.dto.response;

import com.dms.deverytime.domain.comment.entity.Comment;
import java.time.LocalDateTime;

public record CommentCreateResponse(
        Long id,
        Long postId,
        Long userId,
        String content,
        LocalDateTime createdAt
) {
    public static CommentCreateResponse from(Comment comment){
        return new CommentCreateResponse(
                comment.getId(), comment.getPost().getId(), comment.getUser().getId(),
                comment.getContent(), comment.getCreatedAt());
    }
}