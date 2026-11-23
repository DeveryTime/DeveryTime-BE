package com.dms.deverytime.domain.post.dto.response;

import com.dms.deverytime.domain.post.entity.Post;
import com.dms.deverytime.domain.post.enums.PostCreateStatus;
import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(

        // 게시글 상세 조회시 필요 정보
        Long id,
        String title,
        String content,
        PostCreateStatus status,
        int viewCount,
        Long writerId,
        String writerNickname,
        String writerProfileImageUrl,
        String categoryName,
        List<String> imageUrls,
        List<PostImageResponse> images,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long likeCount,
        boolean liked
) {
    public static PostDetailResponse from(Post post, List<String> imageUrls,
                                          List<PostImageResponse> images, long likeCount, boolean liked){
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getStatus(),
                post.getViewCount(),
                post.getUser().getId(),
                post.getUser().getUsername(),
                post.getUser().getProfileImageUrl(),
                post.getCategory().getName(),
                imageUrls,
                images,
                post.getCreatedAt(),
                post.getUpdatedAt(),
                likeCount,
                liked
        );
    }
}
