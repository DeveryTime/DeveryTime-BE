package com.dms.deverytime.domain.post.dto.request;

import com.dms.deverytime.domain.post.enums.PostCreateStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(

        @NotNull
        Long categoryId,

        @NotBlank
        @Size(max = 255)
        String title,

        @NotBlank
        @Size(max = 10000)
        String content,

        @NotNull
        PostCreateStatus status
) {
}
