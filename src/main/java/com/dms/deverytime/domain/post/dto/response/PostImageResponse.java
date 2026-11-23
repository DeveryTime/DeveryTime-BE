package com.dms.deverytime.domain.post.dto.response;

import com.dms.deverytime.domain.post.entity.PostImage;

public record PostImageResponse(Long id, String imageUrl, int sortOrder) {

    // 저장된 PostImage 엔티티를 응답 DTO로 변환
    // publicId는 Cloudinary 삭제용 내부값이라 노출 X
    public static PostImageResponse from(PostImage postImage) {
        return new PostImageResponse(
                postImage.getId(),
                postImage.getImageUrl(),
                postImage.getSortOrder()
        );
    }
}
