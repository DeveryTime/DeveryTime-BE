package com.dms.deverytime.domain.post.dto.request;

import com.dms.deverytime.domain.post.enums.PostCreateStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PostUpdateRequest(

        // 기존 클라이언트가 생략하면 현재 카테고리와 상태를 유지한다.
        @Positive
        Long categoryId,

        PostCreateStatus status,

        @NotBlank
        @Size (max = 255)
        String title,

        @NotBlank
        @Size(max = 10000)
        String content,

        @Size(max = 3)
        List<@NotNull @Positive Long> deleteImageIds
) {
}
