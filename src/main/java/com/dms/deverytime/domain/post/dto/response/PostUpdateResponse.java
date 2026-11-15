package com.dms.deverytime.domain.post.dto.response;

import com.dms.deverytime.domain.post.entity.Post;
import com.dms.deverytime.domain.post.entity.PostCreateStatus;
import java.time.LocalDateTime;

public record PostUpdateResponse(Long id, String title, PostCreateStatus status, LocalDateTime updatedAt) {
    public static PostUpdateResponse from(Post post) {
        return new PostUpdateResponse(post.getId(), post.getTitle(), post.getStatus(), post.getUpdatedAt());
    }
}
