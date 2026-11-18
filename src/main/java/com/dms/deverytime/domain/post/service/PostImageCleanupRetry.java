package com.dms.deverytime.domain.post.service;

import com.dms.deverytime.domain.post.repository.PostImageCleanupRepository;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostImageCleanupRetry {
    private final PostImageCleanupRepository cleanupRepository;
    private final PostImageCleanupService cleanupService;

    @Scheduled(initialDelayString = "${app.post-image-cleanup.retry-delay-ms:300000}",
            fixedDelayString = "${app.post-image-cleanup.retry-delay-ms:300000}")
    public void retryPendingCleanup() {
        for (Long postId : cleanupRepository.findPostIdsWithRetryableCleanup(LocalDateTime.now())) {
            try {
                cleanupService.cleanupAfterContentSaved(postId);
            } catch (RuntimeException failure) {
                log.error("Post image cleanup retry failed for post: {}", postId, failure);
            }
        }
    }
}
