package com.dms.deverytime.domain.post.service;

import com.dms.deverytime.domain.category.entity.Category;
import com.dms.deverytime.domain.category.repository.CategoryRepository;
import com.dms.deverytime.domain.comment.repository.CommentRepository;
import com.dms.deverytime.domain.post.dto.response.PostImageResponse;
import com.dms.deverytime.domain.post.entity.Post;
import com.dms.deverytime.domain.post.entity.PostCreateStatus;
import com.dms.deverytime.domain.post.entity.PostImage;
import com.dms.deverytime.domain.post.repository.*;
import com.dms.deverytime.domain.postlike.repository.PostLikeRepository;
import com.dms.deverytime.domain.user.entity.User;
import com.dms.deverytime.domain.user.repository.UserRepository;
import com.dms.deverytime.global.cloudinary.service.ImageStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostDetailImagesTest {
    @Test
    void detailReturnsImageIdsWhilePreservingLegacyUrlList() {
        User user = User.builder().username("tester").build();
        ReflectionTestUtils.setField(user, "id", 5L);
        Post post = new Post(user, new Category("test"), "title", "content", PostCreateStatus.PUBLISHED);
        ReflectionTestUtils.setField(post, "id", 10L);
        PostImage first = new PostImage(post, "https://example.com/a", "private-a", 0);
        PostImage second = new PostImage(post, "https://example.com/b", "private-b", 1);
        ReflectionTestUtils.setField(first, "id", 21L);
        ReflectionTestUtils.setField(second, "id", 22L);

        PostRepository posts = mock(PostRepository.class);
        PostImageRepository images = mock(PostImageRepository.class);
        PostViewLogRepository views = mock(PostViewLogRepository.class);
        when(posts.findById(10L)).thenReturn(Optional.of(post));
        when(images.findByPostIdOrderBySortOrder(10L)).thenReturn(List.of(first, second));
        when(views.existsByPostIdAndUserId(10L, 5L)).thenReturn(true);
        PostService service = new PostService(posts, mock(UserRepository.class), mock(CategoryRepository.class),
                images, views, mock(PostLikeRepository.class), mock(ImageStorageService.class),
                mock(CommentRepository.class), mock(PostImageCleanupRepository.class), mock(PostImageCleanupService.class));

        var response = service.getPostDetail(10L, 5L);
        assertThat(response.getImageUrls()).containsExactly("https://example.com/a", "https://example.com/b");
        assertThat(response.getImages()).extracting(PostImageResponse::getId).containsExactly(21L, 22L);
        assertThat(response.getImages()).extracting(PostImageResponse::getSortOrder).containsExactly(0, 1);

        when(images.findByPostIdOrderBySortOrder(10L)).thenReturn(List.of());
        var empty = service.getPostDetail(10L, 5L);
        assertThat(empty.getImages()).isEmpty();
        assertThat(empty.getImageUrls()).isEmpty();
    }
}
