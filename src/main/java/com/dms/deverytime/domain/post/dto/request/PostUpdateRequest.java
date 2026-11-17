package com.dms.deverytime.domain.post.dto.request;

import com.dms.deverytime.domain.post.entity.PostCreateStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Getter
public class PostUpdateRequest {

    // 기존 클라이언트가 생략하면 현재 카테고리와 상태를 유지한다.
    @Positive
    private Long categoryId;

    private PostCreateStatus status;

    @NotBlank
    @Size (max = 255)
    private String title;

    @NotBlank
    @Size(max = 10000)
    private String content;

    @Size(max = 3)
    private List<@NotNull @Positive Long> deleteImageIds;

    public PostUpdateRequest() {
    }
}
