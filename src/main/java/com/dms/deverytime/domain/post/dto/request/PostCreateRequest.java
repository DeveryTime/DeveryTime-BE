package com.dms.deverytime.domain.post.dto.request;

import com.dms.deverytime.domain.post.entity.PostCreateStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostCreateRequest {

    @NotBlank
    private Long categoryId;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 7777)
    private String content;

    @NotNull
    private PostCreateStatus status;

    // 기본 생성자
    public PostCreateRequest() {
    }
}
