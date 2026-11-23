package com.dms.deverytime.domain.post.service;

import com.dms.deverytime.domain.category.entity.Category;
import com.dms.deverytime.domain.category.repository.CategoryRepository;
import com.dms.deverytime.domain.comment.repository.CommentRepository;
import com.dms.deverytime.domain.post.dto.request.PostCreateRequest;
import com.dms.deverytime.domain.post.dto.request.PostUpdateRequest;
import com.dms.deverytime.domain.post.dto.response.PostDetailResponse;
import com.dms.deverytime.domain.post.dto.response.PostImageResponse;
import com.dms.deverytime.domain.post.dto.response.PostListResponse;
import com.dms.deverytime.domain.post.dto.response.PostUpdateResponse;
import com.dms.deverytime.domain.post.enums.PostCreateStatus;
import com.dms.deverytime.domain.post.entity.Post;
import com.dms.deverytime.domain.post.entity.PostImage;
import com.dms.deverytime.domain.post.entity.PostImageCleanup;
import com.dms.deverytime.domain.post.entity.PostViewLog;
import com.dms.deverytime.domain.post.repository.PostImageRepository;
import com.dms.deverytime.domain.post.repository.PostImageCleanupRepository;
import com.dms.deverytime.domain.post.repository.PostRepository;
import com.dms.deverytime.domain.post.repository.PostViewLogRepository;
import com.dms.deverytime.domain.postlike.repository.PostLikeRepository;
import com.dms.deverytime.domain.user.entity.User;
import com.dms.deverytime.domain.user.repository.UserRepository;
import com.dms.deverytime.global.cloudinary.dto.ImageUploadResult;
import com.dms.deverytime.global.cloudinary.service.ImageStorageService;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private static final int MAX_POST_IMAGES = 3;

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PostImageRepository postImageRepository;
    private final PostViewLogRepository postViewLogRepository;
    private final PostLikeRepository postLikeRepository;
    private final ImageStorageService imageStorageService;
    private final CommentRepository commentRepository;
    private final PostImageCleanupRepository postImageCleanupRepository;
    private final PostImageCleanupService postImageCleanupService;
    private TransactionTemplate transactionTemplate;

    // 업로드 흐름에서 트랜잭션 경계를 직접 나누기 위해 사용한다. 생성자 시그니처는 유지한다.
    @Autowired
    void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public Long createPost(PostCreateRequest request, Long loginUserId) {
        //User 존재 유무 체크
        User user = userRepository.findById(loginUserId)
                // 찾는 그 User가 없으면 User 없다는 에러코드
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.USER_NOT_FOUND));

        Category category = categoryRepository
                //위와 같이 카테고리 찾고 없으면 없다는 에러코드
                .findById(request.categoryId())
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.CATEGORY_NOT_FOUND));

        //검증된 user, category값과 사용자가 적어 보낸 값으로 post 구성
        Post post = new Post(user, category, request.title(), request.content(), request.status());

        return postRepository.save(post).getId();
    }

    @Transactional (readOnly = true)
    public Page<PostListResponse> getPostList(Long categoryId, Pageable pageable) {

        // categoryId가 있으면 해당 카테고리 조회
        Page<Post> posts = (categoryId != null)
                ? postRepository.findByCategoryIdWithCategory(categoryId, pageable)
                : postRepository.findAllWithCategory(pageable);

        return posts.map(PostListResponse::from);
    }

    @Transactional
    public PostDetailResponse getPostDetail(Long id, Long loginUserId) {

        // 게시글 조회
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.POST_NOT_FOUND));

        // 조회 이력과 이미지 등 부가 정보를 읽기 전에 임시저장 글을 보호한다.
        if (post.getStatus() == PostCreateStatus.DRAFT && !post.getUser().getId().equals(loginUserId)) {
            throw new DeveryTimeException(ErrorCode.POST_NOT_FOUND);
        }

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
        List<PostImageResponse> images = postImageRepository.findByPostIdOrderBySortOrder(id)
                .stream()
                .map(PostImageResponse::from)
                .toList();
        List<String> imageUrls = images.stream().map(PostImageResponse::imageUrl).toList();

        long likeCount = postLikeRepository.countByPostId(id);

        boolean liked =
                postLikeRepository.existsByPostIdAndUserId(id, loginUserId);

        // DTO로 변환
        return PostDetailResponse.from(post, imageUrls,images, likeCount, liked);
    }

    // update, delete id 비교 검증 전용 메서드
    private void validatePostOwner(Post post, Long loginUserId) {
        if (!post.getUser().getId().equals(loginUserId)) {
            throw new DeveryTimeException(ErrorCode.FORBIDDEN);
        }
    }

    @Transactional
    public PostUpdateResponse updatePost(Long postId, PostUpdateRequest request, Long loginUserId) {

        Post post = postRepository.findByIdForImageUpload(postId)
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.POST_NOT_FOUND));

        validatePostOwner(post, loginUserId);

        Category category = request.categoryId() == null ? post.getCategory()
                : categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.CATEGORY_NOT_FOUND));
        PostCreateStatus status = request.status() == null ? post.getStatus() : request.status();

        PostImageReferences references = PostImageReferences.from(request.content());

        // 이미 본문에서 제거한 이전 파일 URL은 뒤늦은 편집 요청으로 되살릴 수 없다.
        for (PostImageCleanup retired : postImageCleanupRepository.findByPostIdAndRetiredTrue(postId)) {
            if (references.contains(retired.getImageUrl())) {
                throw new DeveryTimeException(ErrorCode.POST_IMAGE_CONTENT_CONFLICT);
            }
        }

        List<PostImage> deletedImages = new ArrayList<>();
        if (request.deleteImageIds() != null) {
            for (Long imageId : request.deleteImageIds().stream().distinct().toList()) {
                PostImage image = postImageRepository.findByIdAndPostId(imageId, postId)
                        .orElseThrow(() -> new DeveryTimeException(ErrorCode.POST_IMAGE_NOT_FOUND));
                deletedImages.add(image);
            }
        }
        // 교체 후 아직 본문에 남아 있는 이전 URL도 삭제 전에 함께 검증한다.
        List<PostImageCleanup> pending = postImageCleanupRepository.findByPostIdOrderById(postId);
        for (PostImage image : deletedImages) {
            if (references.contains(image.getImageUrl())
                    || pending.stream().anyMatch(cleanup -> cleanup.getImageId().equals(image.getId())
                    && references.contains(cleanup.getImageUrl()))) {
                throw new DeveryTimeException(ErrorCode.VALIDATION_ERROR);
            }
        }

        for (PostImage image : deletedImages) {
            PostImageCleanup cleanup = new PostImageCleanup(postId, image);
            cleanup.retire();
            postImageCleanupRepository.save(cleanup);
            pending.stream().filter(old -> old.getImageId().equals(image.getId())).forEach(PostImageCleanup::retire);
            postImageRepository.delete(image);
        }

        // 정리 대상 확정은 본문 저장과 함께 커밋한다. 외부 파일 삭제는 그 뒤에 수행한다.
        for (PostImageCleanup cleanup : pending) {
            if (cleanup.isRetired() || references.contains(cleanup.getImageUrl())) {
                continue;
            }
            // 새 사진이 본문에 없어도, 이전 URL이 제거되었으면 이전 파일은 정리한다.
            cleanup.retire();
        }

        post.update(request.title(), request.content(), category, status);
        // @PreUpdate로 확정된 수정 시각을 응답에 담는다.
        postRepository.flush();
        registerReplacedImageCleanup(postId);
        return PostUpdateResponse.from(post);
    }

    @Transactional
    public void deletePost(Long postId, Long loginUserId) {

        Post post = postRepository.findByIdForImageUpload(postId)
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.POST_NOT_FOUND));

        validatePostOwner(post, loginUserId);

        // 게시글이 삭제되어도 외부 파일 삭제 실패를 다시 시도할 수 있도록 기록한다.
        for (PostImage image : postImageRepository.findByPostIdOrderBySortOrder(postId)) {
            PostImageCleanup cleanup = new PostImageCleanup(postId, image);
            cleanup.retire();
            postImageCleanupRepository.save(cleanup);
        }

        // 자식 데이터 먼저 삭제
        commentRepository.deleteByPostId(postId);
        postViewLogRepository.deleteByPostId(postId);
        postImageRepository.deleteByPostId(postId);
        postLikeRepository.deleteByPostId(postId);

        postRepository.delete(post);
        registerReplacedImageCleanup(postId);
    }

    // 게시글 이미지 업로드
    // 외부 트랜잭션이 커넥션을 쥔 채 REQUIRES_NEW 커넥션을 기다리지 않도록 단계를 나눈다.
    //  1) 짧은 트랜잭션으로 검증 -> 2) 트랜잭션 밖에서 정리 기록 예약 -> 3) 본 트랜잭션에서 업로드와 저장
    public List<PostImageResponse> uploadPostImages(Long postId, List<MultipartFile> files,
                                                   Long loginUserId) {
        List<Long> cleanupIds = new ArrayList<>();
        boolean joined = TransactionSynchronizationManager.isActualTransactionActive();
        if (joined) {
            registerUploadRollbackCleanup(cleanupIds);
        }
        try {
            transactionTemplate.executeWithoutResult(status -> {
                lockOwnedPost(postId, loginUserId);
                checkUploadable(postId, files);
                // 잘못된 파일이 있으면 외부 저장소에 업로드하기 전에 전체 요청을 거절한다.
                files.forEach(imageStorageService::validateImage);
            });

            List<String> publicIds = new ArrayList<>();
            for (int i = 0; i < files.size(); i++) {
                String publicId = imageStorageService.newPostImagePublicId();
                cleanupIds.add(postImageCleanupService.recordUploadedFile(postId, publicId));
                publicIds.add(publicId);
            }

            return transactionTemplate.execute(status -> {
                // 검증 이후 상태가 바뀔 수 있으므로 게시글 락을 잡은 뒤 다시 확인한다.
                Post post = lockOwnedPost(postId, loginUserId);
                List<PostImage> existingImages = checkUploadable(postId, files);
                int nextOrder = existingImages.stream()
                        .mapToInt(PostImage::getSortOrder)
                        .max()
                        .orElse(-1) + 1;

                List<PostImageResponse> responses = new ArrayList<>();
                for (int i = 0; i < files.size(); i++) {
                    ImageUploadResult result = imageStorageService.uploadReservedPostImage(
                            files.get(i), publicIds.get(i));
                    PostImage image = postImageRepository.save(new PostImage(
                            post, result.profileImageUrl(), result.profileImagePublicId(), nextOrder++));
                    postImageCleanupRepository.findByIdForUpdate(cleanupIds.get(i)).orElseThrow().complete();
                    responses.add(PostImageResponse.from(image));
                }
                return responses;
            });
        } catch (RuntimeException failure) {
            if (!joined) {
                cleanupRolledBackUploads(cleanupIds);
            }
            throw failure;
        }
    }

    public PostImageResponse replacePostImage(Long postId, Long imageId, MultipartFile file, Long loginUserId) {
        List<Long> cleanupIds = new ArrayList<>();
        boolean joined = TransactionSynchronizationManager.isActualTransactionActive();
        if (joined) {
            registerUploadRollbackCleanup(cleanupIds);
        }
        try {
            transactionTemplate.executeWithoutResult(status -> {
                lockOwnedPost(postId, loginUserId);
                findPostImage(postId, imageId);
                imageStorageService.validateImage(file);
            });

            String publicId = imageStorageService.newPostImagePublicId();
            cleanupIds.add(postImageCleanupService.recordUploadedFile(postId, publicId));

            return transactionTemplate.execute(status -> {
                lockOwnedPost(postId, loginUserId);
                PostImage image = findPostImage(postId, imageId);
                ImageUploadResult result = imageStorageService.uploadReservedPostImage(file, publicId);

                // 편집 중인 클라이언트가 이전 URL을 아직 사용할 수 있으므로 본문 저장 전에는 보존한다.
                postImageCleanupRepository.save(new PostImageCleanup(postId, image));
                image.replaceFile(result.profileImageUrl(), result.profileImagePublicId());
                postImageCleanupRepository.findByIdForUpdate(cleanupIds.get(0)).orElseThrow().complete();
                return PostImageResponse.from(image);
            });
        } catch (RuntimeException failure) {
            if (!joined) {
                cleanupRolledBackUploads(cleanupIds);
            }
            throw failure;
        }
    }

    private Post lockOwnedPost(Long postId, Long loginUserId) {
        Post post = postRepository.findByIdForImageUpload(postId)
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.POST_NOT_FOUND));
        validatePostOwner(post, loginUserId);
        return post;
    }

    private List<PostImage> checkUploadable(Long postId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new DeveryTimeException(ErrorCode.INVALID_IMAGE_FILE);
        }
        List<PostImage> existingImages = postImageRepository.findByPostIdOrderBySortOrder(postId);
        if (files.size() > MAX_POST_IMAGES - existingImages.size()) {
            throw new DeveryTimeException(ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);
        }
        return existingImages;
    }

    private PostImage findPostImage(Long postId, Long imageId) {
        return postImageRepository.findByIdAndPostId(imageId, postId)
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.POST_IMAGE_NOT_FOUND));
    }

    private void registerReplacedImageCleanup(Long postId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    postImageCleanupService.cleanupAfterContentSaved(postId);
                } catch (RuntimeException failure) {
                    log.error("Post saved but replaced-image cleanup failed; records retained for post: {}",
                            postId, failure);
                }
            }
        });
    }

    // 호출자의 트랜잭션에 합류한 경우에만 사용한다. 직접 트랜잭션을 연 경우는 종료 후 호출부에서 정리한다.
    private void registerUploadRollbackCleanup(List<Long> cleanupIds) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_UNKNOWN) {
                    log.error("Image upload transaction outcome unknown; cleanup requires verification: {}",
                            cleanupIds);
                }
                if (status == STATUS_ROLLED_BACK) {
                    cleanupRolledBackUploads(cleanupIds);
                }
            }
        });
    }

    private void cleanupRolledBackUploads(List<Long> cleanupIds) {
        for (Long cleanupId : cleanupIds) {
            try {
                postImageCleanupService.cleanupRolledBackUpload(cleanupId);
            } catch (RuntimeException cleanupFailure) {
                log.error("Rolled-back image cleanup retained for retry: {}", cleanupId, cleanupFailure);
            }
        }
    }

}
