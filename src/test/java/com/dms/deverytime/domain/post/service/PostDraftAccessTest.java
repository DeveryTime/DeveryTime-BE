package com.dms.deverytime.domain.post.service;

import com.dms.deverytime.domain.category.entity.Category;
import com.dms.deverytime.domain.category.repository.CategoryRepository;
import com.dms.deverytime.domain.comment.repository.CommentRepository;
import com.dms.deverytime.domain.post.entity.*;
import com.dms.deverytime.domain.post.repository.*;
import com.dms.deverytime.domain.postlike.repository.PostLikeRepository;
import com.dms.deverytime.domain.user.entity.User;
import com.dms.deverytime.domain.user.repository.UserRepository;
import com.dms.deverytime.global.cloudinary.service.ImageStorageService;
import com.dms.deverytime.global.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostDraftAccessTest {
    private PostService service;
    private Post post;
    private PostViewLogRepository views;
    private PostImageRepository images;
    private PostLikeRepository likes;
    private UserRepository users;

    @BeforeEach
    void setup() {
        User author = User.builder().username("author").build();
        ReflectionTestUtils.setField(author, "id", 5L);
        post = new Post(author, new Category("test"), "draft", "secret", PostCreateStatus.DRAFT);
        ReflectionTestUtils.setField(post, "id", 10L);
        var posts = mock(PostRepository.class);
        views = mock(PostViewLogRepository.class);
        images = mock(PostImageRepository.class);
        likes = mock(PostLikeRepository.class);
        users = mock(UserRepository.class);
        when(posts.findById(10L)).thenReturn(Optional.of(post));
        when(images.findByPostIdOrderBySortOrder(10L)).thenReturn(List.of());
        service = new PostService(posts, users, mock(CategoryRepository.class), images, views, likes,
                mock(ImageStorageService.class), mock(CommentRepository.class),
                mock(PostImageCleanupRepository.class), mock(PostImageCleanupService.class));
    }

    @Test
    void otherUserCannotReadDraftAndDoesNotIncrementViews() {
        assertThatThrownBy(() -> service.getPostDetail(10L, 6L))
                .isInstanceOfSatisfying(DeveryTimeException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND));
        assertThat(post.getViewCount()).isZero();
        verifyNoInteractions(views, images, likes, users);
    }

    @Test
    void authorCanReadDraft() {
        when(views.existsByPostIdAndUserId(10L, 5L)).thenReturn(true);
        assertThat(service.getPostDetail(10L, 5L).getContent()).isEqualTo("secret");
    }

    @Test
    void otherUserCanReadPublishedPostAndFirstViewIsRecorded() {
        ReflectionTestUtils.setField(post, "status", PostCreateStatus.PUBLISHED);
        User viewer = User.builder().username("viewer").build();
        ReflectionTestUtils.setField(viewer, "id", 6L);
        when(users.findById(6L)).thenReturn(Optional.of(viewer));
        assertThat(service.getPostDetail(10L, 6L).getViewCount()).isEqualTo(1);
        verify(views).save(any(PostViewLog.class));
    }
}
