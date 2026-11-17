package com.dms.deverytime.domain.post.service;

import com.dms.deverytime.domain.category.entity.Category;
import com.dms.deverytime.domain.category.repository.CategoryRepository;
import com.dms.deverytime.domain.comment.repository.CommentRepository;
import com.dms.deverytime.domain.post.dto.request.PostCreateRequest;
import com.dms.deverytime.domain.post.entity.Post;
import com.dms.deverytime.domain.post.entity.PostCreateStatus;
import com.dms.deverytime.domain.post.repository.*;
import com.dms.deverytime.domain.postlike.repository.PostLikeRepository;
import com.dms.deverytime.domain.user.entity.User;
import com.dms.deverytime.domain.user.repository.UserRepository;
import com.dms.deverytime.global.cloudinary.service.ImageStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostMarkdownContentTest {
    @Test
    void createSavesMarkdownStringWithoutConvertingToHtml() {
        var posts = mock(PostRepository.class);
        var users = mock(UserRepository.class);
        var categories = mock(CategoryRepository.class);
        var user = User.builder().username("tester").build();
        var category = new Category("test");
        when(users.findById(5L)).thenReturn(Optional.of(user));
        when(categories.findById(3L)).thenReturn(Optional.of(category));
        when(posts.save(any(Post.class))).thenAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });
        var service = new PostService(posts, users, categories, mock(PostImageRepository.class),
                mock(PostViewLogRepository.class), mock(PostLikeRepository.class), mock(ImageStorageService.class),
                mock(CommentRepository.class), mock(PostImageCleanupRepository.class), mock(PostImageCleanupService.class));
        String markdown = "## 제목\n\n**굵게**, *기울임*, <u>밑줄</u>\n\n[링크](https://example.com)\n![사진](https://example.com/a.png)";
        var request = new PostCreateRequest();
        request.setCategoryId(3L);
        request.setTitle("title");
        request.setContent(markdown);
        request.setStatus(PostCreateStatus.PUBLISHED);
        assertThat(service.createPost(request, 5L)).isEqualTo(10L);
        var saved = ArgumentCaptor.forClass(Post.class);
        verify(posts).save(saved.capture());
        assertThat(saved.getValue().getContent()).isEqualTo(markdown);
    }
}
