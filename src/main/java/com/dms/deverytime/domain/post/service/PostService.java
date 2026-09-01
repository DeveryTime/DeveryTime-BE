package com.dms.deverytime.domain.post.service;

import com.dms.deverytime.domain.category.entity.Category;
import com.dms.deverytime.domain.category.repository.CategoryRepository;
import com.dms.deverytime.domain.post.dto.request.PostRequest;
import com.dms.deverytime.domain.post.dto.response.PostDetailResponse;
import com.dms.deverytime.domain.post.dto.response.PostListResponse;
import com.dms.deverytime.domain.post.entity.Post;
import com.dms.deverytime.domain.post.entity.PostImage;
import com.dms.deverytime.domain.post.entity.PostViewLog;
import com.dms.deverytime.domain.post.repository.PostImageRepository;
import com.dms.deverytime.domain.post.repository.PostRepository;
import com.dms.deverytime.domain.post.repository.PostViewLogRepository;
import com.dms.deverytime.domain.user.entity.User;
import com.dms.deverytime.domain.user.repository.UserRepository;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;

import java.util.List;

//조회가 더 많으므로 기본값을 readOnly를 사용해 더티채킹을 막기
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PostImageRepository postImageRepository;
    private final PostViewLogRepository postViewLogRepository;

    @Transactional
    public Long createPost(PostRequest request, Long loginUserId) {
        //User 존재 유무 체크
        User user = userRepository.findById(loginUserId)
                // 찾는 그 User가 없으면 User 없다는 에러코드
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.USER_NOT_FOUND));

        Category category = categoryRepository
                //위와 같이 카테고리 찾고 없으면 없다는 에러코드
                .findById(request.getCategoryId())
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.CATEGORY_NOT_FOUND));

        //검증된 user, category값과 사용자가 적어 보낸 값으로 post 구성
        Post post = new Post(user, category, request.getTitle(), request.getContent(), request.getStatus());

        return postRepository.save(post).getId();
    }

    public Page<PostListResponse> getPostList(Pageable pageable) {

        // 페이징 조건으로 게시글 목록 조회
        Page<Post> posts = postRepository.findAllWithCategory(pageable);

        //DTO로 변환
        return posts.map(post -> new PostListResponse(
                post.getId(),
                post.getTitle(),
                post.getCategory().getName(), // ← 주의: category는 LAZY라서 여기서 실제 쿼리가 나감 (N+1 위험 지점)
                post.getCreatedAt()
        ));
    }

    @Transactional
    public PostDetailResponse getPostDetail(Long id, Long loginUserId) {

        // 1. 게시글 조회 — 없으면 POST_NOT_FOUND
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.POST_NOT_FOUND));

        // 2. 이 유저가 이 게시글을 이미 조회한 적 있는지 확인
        boolean alreadyViewed = postViewLogRepository.existsByPostIdAndUserId(id, loginUserId);

        // 3. 처음 보는 거면 조회수 증가 + 조회 로그 남기기
        if (!alreadyViewed) {
            post.increaseViewCount();

            User viewer = userRepository.findById(loginUserId)
                    .orElseThrow(() -> new DeveryTimeException(ErrorCode.USER_NOT_FOUND));

            postViewLogRepository.save(new PostViewLog(post, viewer));
        }

        // 4. 이 게시글에 달린 이미지 URL 목록 조회 (sortOrder 순으로)
        List<String> imageUrls = postImageRepository.findByPostIdOrderBySortOrder(id)
                .stream()
                .map(PostImage::getImageUrl)
                .toList();

        // 5. DTO로 변환해서 반환
        //    TODO: writerNickname에 name(실명) vs username(아이디) 중 뭘 쓸지 프론트 확인 필요
        //    post.getUser(), post.getCategory()는 LAZY라서 여기서 실제 쿼리가 나감
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getStatus(),
                post.getViewCount(),
                post.getUser().getName(),
                post.getUser().getProfileImageUrl(),
                post.getCategory().getName(),
                imageUrls,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

}
