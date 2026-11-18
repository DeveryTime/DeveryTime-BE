package com.dms.deverytime.domain.post.service;

import com.dms.deverytime.domain.post.entity.Post;
import com.dms.deverytime.domain.post.entity.PostImageCleanup;
import com.dms.deverytime.domain.post.repository.PostImageCleanupRepository;
import com.dms.deverytime.domain.post.repository.PostImageRepository;
import com.dms.deverytime.domain.post.repository.PostRepository;
import com.dms.deverytime.global.cloudinary.service.ImageStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Clock;

@Slf4j
@Service
public class PostImageCleanupService {
    private final PostRepository postRepository;
    private final PostImageCleanupRepository cleanupRepository;
    private final PostImageRepository imageRepository;
    private final ImageStorageService storage;

    private final Clock clock;

    @Autowired
    public PostImageCleanupService(PostRepository postRepository, PostImageCleanupRepository cleanupRepository,
                                   PostImageRepository imageRepository, ImageStorageService storage) {
        this(postRepository, cleanupRepository, imageRepository, storage, Clock.systemDefaultZone());
    }

    PostImageCleanupService(PostRepository postRepository, PostImageCleanupRepository cleanupRepository,
                            PostImageRepository imageRepository, ImageStorageService storage, Clock clock) {
        this.postRepository = postRepository;
        this.cleanupRepository = cleanupRepository;
        this.imageRepository = imageRepository;
        this.storage = storage;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long recordUploadedFile(Long postId, String publicId) {
        return cleanupRepository.saveAndFlush(new PostImageCleanup(postId, publicId)).getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupRolledBackUpload(Long id) {
        PostImageCleanup pending = cleanupRepository.findByIdForUpdate(id).orElse(null);
        if (pending == null || pending.isCompleted() || !pending.isUploadCandidate()) {
            return;
        }
        if (!imageRepository.existsByPublicId(pending.getPublicId())) {
            try {
                if (!storage.deleteUploadCandidate(pending.getPublicId())) {
                    deferMissingCandidate(pending);
                    return;
                }
            } catch (RuntimeException failure) {
                log.error("Failed to delete rolled-back upload; cleanup record retained: {}", id, failure);
                return;
            }
        }
        pending.complete();
    }

    private void deferMissingCandidate(PostImageCleanup pending) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (pending.getRetryDeadline() == null) {
            pending.setRetryDeadline(now.plusDays(7));
        }
        if (!pending.getRetryDeadline().isAfter(now)) {
            // 파일이 없는 상태가 7일 지속된 경우 자동 재시도를 종료한다.
            log.warn("Upload cleanup expired after seven days: id={}, publicId={}",
                    pending.getId(), pending.getPublicId());
            pending.complete();
        } else {
            pending.deferRetry(now.plusHours(6));
        }
    }

    // 본문 저장 트랜잭션이 확정된 후 별도 트랜잭션으로 처리한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupAfterContentSaved(Long postId) {
        Post post = postRepository.findByIdForImageUpload(postId).orElse(null);
        for (PostImageCleanup pending : cleanupRepository.findPendingForUpdate(postId)) {
            if (pending.isUploadCandidate() && imageRepository.existsByPublicId(pending.getPublicId())) {
                pending.complete();
                continue;
            }
            if (post != null && !pending.isRetired() && !pending.isUploadCandidate()) {
                continue;
            }
            if (pending.isUploadCandidate() && pending.getNextRetryAt() != null
                    && pending.getNextRetryAt().isAfter(LocalDateTime.now(clock))) {
                continue;
            }
            try {
                if (pending.isUploadCandidate()) {
                    if (!storage.deleteUploadCandidate(pending.getPublicId())) {
                        deferMissingCandidate(pending);
                        continue;
                    }
                } else {
                    storage.delete(pending.getPublicId());
                }
                // 이력은 지우지 않는다. 외부 삭제 후 DB 커밋이 실패해도 retire 기록은 이미 확정되어 있다.
                pending.complete();
            } catch (RuntimeException failure) {
                // 실패한 기록은 정기 재시도 시 다시 처리한다.
                log.error("Failed to delete replaced post image; cleanup record retained: {}",
                        pending.getId(), failure);
            }
        }
    }

}
