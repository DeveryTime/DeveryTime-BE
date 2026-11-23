package com.dms.deverytime.domain.search.dto.response;

import com.dms.deverytime.domain.post.entity.Post;
import java.time.LocalDateTime;

// 검색 전용 응답 DTO
// PostListResponse에 필드를 추가하면 게시글 목록·유저별 게시글 API까지 영향을 받으므로 분리
public record PostSearchInfo(

        Long id,
        String title,
        Long writerId,
        // 작성자 아바타 표시용, 없으면 null
        String authorProfileImageUrl,
        String categoryName,
        LocalDateTime createdAt,
        int likeCount
) {

    // user, category는 Repository에서 fetch join으로 함께 조회된 상태여야 함 (N+1 방지)
    public static PostSearchInfo from(Post post) {
        return new PostSearchInfo(
                post.getId(),
                post.getTitle(),
                post.getUser().getId(),
                post.getUser().getProfileImageUrl(),
                post.getCategory().getName(),
                post.getCreatedAt(),
                post.getLikeCount()
        );
    }
}
