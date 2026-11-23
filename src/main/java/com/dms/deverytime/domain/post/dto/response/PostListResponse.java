package com.dms.deverytime.domain.post.dto.response;

import com.dms.deverytime.domain.post.entity.Post;

import java.time.LocalDateTime;

public record PostListResponse(

        //목록 조회 때 필요한 것들
        Long id,
        String title,
        Long writerId,
        String categoryName,
        LocalDateTime createdAt,
        int likeCount
) {
    public static PostListResponse from(Post post){
        return new PostListResponse(post.getId(), post.getTitle(),
                post.getUser().getId(), post.getCategory().getName(),
                post.getCreatedAt(), post.getLikeCount());
    }
}
