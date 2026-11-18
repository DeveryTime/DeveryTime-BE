package com.dms.deverytime.domain.post.service;

import com.dms.deverytime.domain.post.entity.Post;
import com.dms.deverytime.domain.post.entity.PostImageCleanup;
import com.dms.deverytime.domain.post.repository.PostImageCleanupRepository;
import com.dms.deverytime.domain.post.repository.PostImageRepository;
import com.dms.deverytime.domain.post.repository.PostRepository;
import com.dms.deverytime.global.cloudinary.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.jsoup.parser.Parser;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostImageCleanupService {
    private final PostRepository postRepository;
    private final PostImageCleanupRepository cleanupRepository;
    private final PostImageRepository imageRepository;
    private final ImageStorageService storage;

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
        LocalDateTime now = LocalDateTime.now();
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
                    && pending.getNextRetryAt().isAfter(LocalDateTime.now())) {
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

    // 본문은 Markdown 문자열이다. JSON으로 직렬화된 기존 본문도 호환한다.
    static boolean referencesImageInRequest(String content, String imageUrl) {
        if (content == null) {
            return false;
        }
        // 유효한 JSON이어도 객체 키에 사진 URL이 포함될 수 있다.
        if (containsImageUrl(content, imageUrl)) {
            return true;
        }
        try {
            return referencesImage(JsonMapper.shared().readTree(content), imageUrl);
        } catch (JacksonException notJson) {
            return false;
        }
    }

    static boolean referencesImage(String content, String imageUrl) {
        // [링크](URL), 인용문 등으로 시작하는 Markdown도 정상적인 본문이다.
        return referencesImageInRequest(content, imageUrl);
    }

    private static boolean referencesImage(JsonNode node, String imageUrl) {
        if (node == null) {
            return false;
        }
        if (node.isString()) {
            return containsImageUrl(node.stringValue(), imageUrl);
        }
        if (node.isObject()) {
            for (var property : node.properties()) {
                if (containsImageUrl(property.getKey(), imageUrl)) {
                    return true;
                }
            }
        }
        for (JsonNode child : node.values()) {
            if (referencesImage(child, imageUrl)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsImageUrl(String text, String imageUrl) {
        String unescaped = Parser.unescapeEntities(text, false);
        if (unescaped.contains(imageUrl)) {
            return true;
        }
        // CommonMark에서는 ASCII 문장부호 앞의 백슬래시를 제거한다.
        StringBuilder normalized = new StringBuilder(unescaped.length());
        for (int i = 0; i < unescaped.length(); i++) {
            char current = unescaped.charAt(i);
            if (current == '\\' && i + 1 < unescaped.length() && isAsciiPunctuation(unescaped.charAt(i + 1))) {
                current = unescaped.charAt(++i);
            }
            normalized.append(current);
        }
        return normalized.toString().contains(imageUrl);
    }

    private static boolean isAsciiPunctuation(char c) {
        return (c >= '!' && c <= '/') || (c >= ':' && c <= '@')
                || (c >= '[' && c <= '`') || (c >= '{' && c <= '~');
    }
}
