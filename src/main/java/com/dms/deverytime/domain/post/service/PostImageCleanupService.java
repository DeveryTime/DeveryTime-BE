package com.dms.deverytime.domain.post.service;

import com.dms.deverytime.domain.post.entity.Post;
import com.dms.deverytime.domain.post.entity.PostImage;
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
import org.springframework.web.util.HtmlUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostImageCleanupService {
    private final PostRepository postRepository;
    private final PostImageRepository imageRepository;
    private final PostImageCleanupRepository cleanupRepository;
    private final ImageStorageService storage;

    // 본문 저장 트랜잭션이 확정된 후 별도 트랜잭션으로 처리한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupAfterContentSaved(Long postId) {
        Post post = postRepository.findByIdForImageUpload(postId).orElse(null);
        for (PostImageCleanup pending : cleanupRepository.findByPostIdOrderById(postId)) {
            if (post != null) {
                PostImage current = imageRepository.findByIdAndPostId(pending.getImageId(), postId).orElse(null);
                // 새 URL 반영과 이전 URL 제거를 모두 확인한 경우에만 이전 파일을 삭제한다.
                if (current == null || referencesImage(post.getContent(), pending.getImageUrl())
                        || !referencesImage(post.getContent(), current.getImageUrl())) {
                    continue;
                }
            }
            try {
                storage.delete(pending.getPublicId());
                cleanupRepository.delete(pending);
            } catch (RuntimeException failure) {
                // 기록을 남겨 다음 본문 저장 때 재시도한다. 본문 저장 성공을 실패로 바꾸지 않는다.
                log.error("Failed to delete replaced post image; cleanup record retained: {}",
                        pending.getId(), failure);
            }
        }
    }

    private boolean referencesImage(String content, String imageUrl) {
        if (content == null) {
            return false;
        }
        String trimmed = content.stripLeading();
        if (trimmed.startsWith("{") || trimmed.startsWith("[") || trimmed.startsWith("\"")) {
            try {
                return referencesImage(JsonMapper.shared().readTree(content), imageUrl);
            } catch (JacksonException invalidJson) {
                // 해석할 수 없는 본문은 파일 삭제를 보류한다.
                return true;
            }
        }
        return HtmlUtils.htmlUnescape(content).contains(imageUrl);
    }

    private boolean referencesImage(JsonNode node, String imageUrl) {
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
