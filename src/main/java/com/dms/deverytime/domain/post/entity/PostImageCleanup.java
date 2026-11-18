package com.dms.deverytime.domain.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    // 본문에서 제거되었다는 사실은 파일 삭제 전에 원래 저장 트랜잭션에서 확정한다.
    @Column(nullable = false)
    private boolean retired;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "upload_candidate", nullable = false)
    private boolean uploadCandidate;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "retry_deadline")
    private LocalDateTime retryDeadline;

    public void setRetryDeadline(LocalDateTime retryDeadline) {
        this.retryDeadline = retryDeadline;
    }

    public void deferRetry(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public void retire() {
        this.retired = true;
    }

    public void complete() {
        this.completed = true;
    }

    public PostImageCleanup(Long postId, PostImage image) {
        this.postId = postId;
        this.imageId = image.getId();
        this.imageUrl = image.getImageUrl();
        this.publicId = image.getPublicId();
    }

    public PostImageCleanup(Long postId, String publicId) {
        this.postId = postId;
        this.imageId = 0L;
        this.imageUrl = "";
        this.publicId = publicId;
        this.uploadCandidate = true;
    }
}
