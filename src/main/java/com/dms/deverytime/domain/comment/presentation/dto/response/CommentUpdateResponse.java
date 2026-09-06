package com.dms.deverytime.domain.comment.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CommentUpdateResponse {
    private Long id;
    private  String content;
    private LocalDateTime updatedAt;
}
