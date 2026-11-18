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
import org.jsoup.parser.Parser;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.time.Clock;
import java.util.regex.Pattern;

@Slf4j
@Service
public class PostImageCleanupService {
    private final PostRepository postRepository;
    private final PostImageCleanupRepository cleanupRepository;
    private final PostImageRepository imageRepository;
    private final ImageStorageService storage;

    private final Clock clock;
    private static final Pattern ENTITY = Pattern.compile("&(?:#[xX][0-9A-Fa-f]{1,8}|#[0-9]{1,8}|[A-Za-z][A-Za-z0-9]{1,31});");
    private static final Pattern HTML_TAG = Pattern.compile("(?s)<[^>]+>");

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
        var matcher = ENTITY.matcher(text);
        StringBuffer markdown = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(markdown, java.util.regex.Matcher.quoteReplacement(
                    Parser.unescapeEntities(matcher.group(), false)));
        }
        matcher.appendTail(markdown);
        if (containsNormalized(markdown.toString(), imageUrl)) return true;

        // HTML 속성에서는 세미콜론 없는 문자 참조도 브라우저 규칙에 따라 처리한다.
        var tags = HTML_TAG.matcher(text);
        while (tags.find()) {
            if (containsNormalized(Parser.unescapeEntities(tags.group(), true), imageUrl)) return true;
        }
        return false;
    }

    private static boolean containsNormalized(String text, String imageUrl) {
        StringBuilder normalized = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == '\\' && i + 1 < text.length() && isAsciiPunctuation(text.charAt(i + 1))) {
                current = text.charAt(++i);
            }
            normalized.append(current);
        }
        return decodeUnreserved(normalized.toString()).contains(decodeUnreserved(imageUrl));
    }

    private static String decodeUnreserved(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '%' && i + 2 < value.length()) {
                int high = Character.digit(value.charAt(i + 1), 16);
                int low = Character.digit(value.charAt(i + 2), 16);
                int c = high < 0 || low < 0 ? -1 : (high << 4) + low;
                if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                        || c == '-' || c == '.' || c == '_' || c == '~') {
                    result.append((char) c);
                    i += 2;
                    continue;
                }
            }
            result.append(value.charAt(i));
        }
        return result.toString();
    }

    private static boolean isAsciiPunctuation(char c) {
        return (c >= '!' && c <= '/') || (c >= ':' && c <= '@')
                || (c >= '[' && c <= '`') || (c >= '{' && c <= '~');
    }
}
