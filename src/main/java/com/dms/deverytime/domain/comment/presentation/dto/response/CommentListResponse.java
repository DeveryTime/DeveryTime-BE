package com.dms.deverytime.domain.comment.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;
@Getter
@AllArgsConstructor

public class CommentListResponse {
    private Long id;
    private Long userId;
    private String authorName;
    private String profileImageUrl;
    private String content;
    private LocalDateTime createdAt;
}
