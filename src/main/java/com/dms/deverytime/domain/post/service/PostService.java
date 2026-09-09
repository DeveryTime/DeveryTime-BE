package com.dms.deverytime.domain.post.service;

import com.dms.deverytime.domain.category.entity.Category;
import com.dms.deverytime.domain.category.repository.CategoryRepository;
import com.dms.deverytime.domain.post.dto.request.PostCreateRequest;
import com.dms.deverytime.domain.post.dto.request.PostUpdateRequest;
import com.dms.deverytime.domain.post.dto.response.PostDetailResponse;
import com.dms.deverytime.domain.post.dto.response.PostListResponse;
import com.dms.deverytime.domain.post.entity.Post;
import com.dms.deverytime.domain.post.entity.PostImage;
import com.dms.deverytime.domain.post.entity.PostViewLog;
import com.dms.deverytime.domain.post.repository.PostImageRepository;
import com.dms.deverytime.domain.post.repository.PostRepository;
import com.dms.deverytime.domain.post.repository.PostViewLogRepository;
import com.dms.deverytime.domain.postlike.repository.PostLikeRepository;
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

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PostImageRepository postImageRepository;
    private final PostViewLogRepository postViewLogRepository;
    private final PostLikeRepository postLikeRepository;

    @Transactional
    public Long createPost(PostCreateRequest request, Long loginUserId) {
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

    @Transactional (readOnly = true)
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

        // 게시글 조회
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.POST_NOT_FOUND));

        // 조회 이력 확인
        boolean alreadyViewed = postViewLogRepository.existsByPostIdAndUserId(id, loginUserId);

        // 처음이면 조회수 ++
        if (!alreadyViewed) {
            post.increaseViewCount();

            User viewer = userRepository.findById(loginUserId)
                    .orElseThrow(() -> new DeveryTimeException(ErrorCode.USER_NOT_FOUND));

            postViewLogRepository.save(new PostViewLog(post, viewer));
        }

        // 이미지 URL 목록 조회
        List<String> imageUrls = postImageRepository.findByPostIdOrderBySortOrder(id)
                .stream()
                .map(PostImage::getImageUrl)
                .toList();

        long likeCount = postLikeRepository.countByPostId(id);

        boolean liked =
                postLikeRepository.existsByPostIdAndUserId(id, loginUserId);

        // DTO로 변환
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
                post.getUpdatedAt(),
                likeCount,
                liked
        );
    }

    // update, delete id 비교 검증 전용 메서드
    private void validatePostOwner(Post post, Long loginUserId) {
        if (!post.getUser().getId().equals(loginUserId)) {
            throw new DeveryTimeException(ErrorCode.FORBIDDEN);
        }
    }

    @Transactional
    public void updatePost(Long postId, PostUpdateRequest request, Long loginUserId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.POST_NOT_FOUND));

       validatePostOwner(post, loginUserId);

        // 제목 내용 수정
        post.update(request.getTitle(), request.getContent());
        postRepository.save(post);
    } // @PreUpdate가 수정일시 자동 반영, 더티체킹으로 별도 코드 "postRepository.save(post);" 없어도 기능적으론 =.

    @Transactional
    public void deletePost(Long postId, Long loginUserId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.POST_NOT_FOUND));

        validatePostOwner(post, loginUserId);

        // 자식 데이터 먼저 삭제
        postViewLogRepository.deleteByPostId(postId);
        postImageRepository.deleteByPostId(postId);
        postLikeRepository.deleteByPostId(postId);

        postRepository.delete(post);
    }

}
