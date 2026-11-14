package com.dms.deverytime.domain.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

@Getter
public class PostUpdateRequest {

    @NotBlank
    @Size (max = 255)
    private String title;

    @NotBlank
    private String content;

    @Size(max = 3)
    private List<@NotNull @Positive Long> deleteImageIds;

    public PostUpdateRequest() {
    }
}
