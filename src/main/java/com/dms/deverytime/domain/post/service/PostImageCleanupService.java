package com.dms.deverytime.domain.post.service;

import com.dms.deverytime.domain.post.entity.Post;
import com.dms.deverytime.domain.post.entity.PostImageCleanup;
import com.dms.deverytime.domain.post.repository.PostImageCleanupRepository;
import com.dms.deverytime.domain.post.repository.PostRepository;
import com.dms.deverytime.global.cloudinary.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostImageCleanupService {
    private final PostRepository postRepository;
    private final PostImageCleanupRepository cleanupRepository;
    private final ImageStorageService storage;

    // 본문 저장 트랜잭션이 확정된 후 별도 트랜잭션으로 처리한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupAfterContentSaved(Long postId) {
        Post post = postRepository.findByIdForImageUpload(postId).orElse(null);
        for (PostImageCleanup pending : cleanupRepository.findByPostIdOrderById(postId)) {
            if (post != null && !pending.isRetired()) {
                continue;
            }
            try {
                storage.delete(pending.getPublicId());
                // 이력은 지우지 않는다. 외부 삭제 후 DB 커밋이 실패해도 retire 기록은 이미 확정되어 있다.
                pending.complete();
            } catch (RuntimeException failure) {
                // 기록을 남겨 다음 본문 저장 때 재시도한다. 본문 저장 성공을 실패로 바꾸지 않는다.
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
        try {
            return referencesImage(JsonMapper.shared().readTree(content), imageUrl);
        } catch (JacksonException notJson) {
            return HtmlUtils.htmlUnescape(content).replace("\\/", "/").contains(imageUrl);
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
            return HtmlUtils.htmlUnescape(node.stringValue()).contains(imageUrl);
        }
        for (JsonNode child : node.values()) {
            if (referencesImage(child, imageUrl)) {
                return true;
            }
        }
        return false;
    }
}
