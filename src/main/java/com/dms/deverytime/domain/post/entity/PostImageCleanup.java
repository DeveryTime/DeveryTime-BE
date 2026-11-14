package com.dms.deverytime.domain.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 교체 직후에는 본문이 이전 URL을 사용할 수 있어, 정리할 파일을 영속적으로 보관한다.
@Entity
@Table(name = "post_image_cleanup", indexes = @Index(name = "idx_post_image_cleanup_post", columnList = "post_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImageCleanup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "image_id", nullable = false)
    private Long imageId;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "public_id", nullable = false)
    private String publicId;

    public PostImageCleanup(Long postId, PostImage image) {
        this.postId = postId;
        this.imageId = image.getId();
        this.imageUrl = image.getImageUrl();
        this.publicId = image.getPublicId();
    }
}
