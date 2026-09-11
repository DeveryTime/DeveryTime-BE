package com.dms.deverytime.domain.search.service;

import com.dms.deverytime.domain.post.dto.response.PostListResponse;
import com.dms.deverytime.domain.post.entity.Post;
import com.dms.deverytime.domain.post.repository.PostRepository;
import com.dms.deverytime.domain.search.dto.request.PostSearchRequest;
import com.dms.deverytime.domain.search.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 다 readonly 이므로 위에서 수식
public class SearchService {

    private final PostRepository postRepository;

    public PageResponse<PostListResponse> searchPostsByTitle(PostSearchRequest request) {
        Pageable pageable = request.toPageable();

        Page<Post> posts = postRepository.findByTitleContainingWithCategory(request.getKeyword(), pageable);

        Page<PostListResponse> mapped = posts.map(post -> new PostListResponse(
                post.getId(),
                post.getTitle(),
                post.getCategory().getName(),
                post.getCreatedAt()
        ));

        // Spring 기본 Page 요소들을 우리가 원하는 5개 필드만 있는 PageResponse로 재포장
        return new PageResponse<>(mapped);
    }

    public PageResponse<PostListResponse> searchPostsByUsername(PostSearchRequest request) {
        Pageable pageable = request.toPageable();

        Page<Post> posts = postRepository.findByUserUsernameContainingWithCategory(request.getKeyword(), pageable);

        Page<PostListResponse> mapped = posts.map(post -> new PostListResponse(
                post.getId(),
                post.getTitle(),
                post.getCategory().getName(),
                post.getCreatedAt()
        ));

        return new PageResponse<>(mapped);
    }
}