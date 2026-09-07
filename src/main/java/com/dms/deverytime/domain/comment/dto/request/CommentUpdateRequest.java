package com.dms.deverytime.domain.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentUpdateRequest (
    @Size(max = 500)
    @NotBlank(message = "댓글 내용은 필수입니다")
    String content
){
}
