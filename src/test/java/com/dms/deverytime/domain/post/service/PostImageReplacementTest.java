package com.dms.deverytime.domain.post.service;

import com.dms.deverytime.domain.category.entity.Category;
import com.dms.deverytime.domain.post.dto.request.PostUpdateRequest;
import com.dms.deverytime.domain.post.entity.Post;
import com.dms.deverytime.domain.post.entity.PostCreateStatus;
import com.dms.deverytime.domain.post.entity.PostImage;
import com.dms.deverytime.domain.post.repository.PostImageCleanupRepository;
import com.dms.deverytime.domain.post.repository.PostImageRepository;
import com.dms.deverytime.domain.user.entity.User;
import com.dms.deverytime.global.cloudinary.dto.ImageUploadResult;
import com.dms.deverytime.global.cloudinary.service.ImageStorageService;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(PostImageUploadTest.Config.class)
class PostImageReplacementTest {
    @Autowired PostService service;
    @Autowired PostImageRepository images;
    @Autowired PostImageCleanupRepository pending;
    @Autowired ImageStorageService storage;
    @Autowired EntityManagerFactory factory;
    @Autowired PlatformTransactionManager transactionManager;

    private static final String OLD_URL = "https://example.com/old.png";
    private static final String NEW_URL = "https://example.com/new.png";
    private static final AtomicInteger sequence = new AtomicInteger(1000);
    private TransactionTemplate transaction;
    private EntityManager em;
    private Long postId;
    private Long userId;
    private Long imageId;

    @BeforeEach
    void setUp() {
        reset(storage);
        transaction = new TransactionTemplate(transactionManager);
        em = SharedEntityManagerCreator.createSharedEntityManager(factory);
        transaction.executeWithoutResult(status -> {
            int n = sequence.incrementAndGet();
            User user = User.builder().schoolYear(2026).schoolNumber(String.valueOf(n))
                    .name("tester").email("replace" + n + "@example.com")
                    .username("r" + n).passwordHash("test").build();
            Category category = new Category("replace" + n);
            em.persist(user);
            em.persist(category);
            Post post = new Post(user, category, "title", body(OLD_URL), PostCreateStatus.PUBLISHED);
            em.persist(post);
            PostImage image = new PostImage(post, OLD_URL, "old-file", 4);
            em.persist(image);
            postId = post.getId();
            userId = user.getId();
            imageId = image.getId();
        });
        when(storage.upload(any(), eq("posts"))).thenReturn(ImageUploadResult.of(NEW_URL, "new-file"));
    }

    @Test
    void replacesImageEvenWhenThreeExistAndPreservesIdsOrderAndOtherImages() {
        transaction.executeWithoutResult(status -> {
            Post post = em.getReference(Post.class, postId);
            em.persist(new PostImage(post, "https://example.com/second", "second", 5));
            em.persist(new PostImage(post, "https://example.com/third", "third", 6));
        });
        var response = service.replacePostImage(postId, imageId, file(), userId);

        assertThat(response.getId()).isEqualTo(imageId);
        assertThat(response.getSortOrder()).isEqualTo(4);
        assertThat(response.getImageUrl()).isEqualTo(NEW_URL);
        assertThat(images.findByPostIdOrderBySortOrder(postId)).extracting(PostImage::getPublicId)
                .containsExactly("new-file", "second", "third");
        assertThat(pending.findByPostIdOrderById(postId)).hasSize(1);
        assertThat(savedContent()).isEqualTo(body(OLD_URL));
        verify(storage, never()).delete(anyString());
    }

    @Test
    void rejectsUnknownPostNonOwnerAndUnknownImageBeforeUploading() {
        assertError(() -> service.replacePostImage(-1L, imageId, file(), userId), ErrorCode.POST_NOT_FOUND);
        assertError(() -> service.replacePostImage(postId, imageId, file(), -1L), ErrorCode.FORBIDDEN);
        assertError(() -> service.replacePostImage(postId, -1L, file(), userId), ErrorCode.POST_IMAGE_NOT_FOUND);
        verify(storage, never()).upload(any(), anyString());
    }

    @Test
    void rejectsImageBelongingToAnotherPost() {
        Long anotherPostId = transaction.execute(status -> {
            Post original = em.find(Post.class, postId);
            Post another = new Post(original.getUser(), original.getCategory(), "another", "content",
                    PostCreateStatus.PUBLISHED);
            em.persist(another);
            return another.getId();
        });
        assertError(() -> service.replacePostImage(anotherPostId, imageId, file(), userId),
                ErrorCode.POST_IMAGE_NOT_FOUND);
        verify(storage, never()).upload(any(), anyString());
    }

    @Test
    void invalidFileAndUploadFailureKeepOldImage() {
        doThrow(new DeveryTimeException(ErrorCode.INVALID_IMAGE_FILE)).when(storage).validateImage(null);
        assertError(() -> service.replacePostImage(postId, imageId, null, userId), ErrorCode.INVALID_IMAGE_FILE);
        when(storage.upload(any(), eq("posts"))).thenThrow(new DeveryTimeException(ErrorCode.IMAGE_UPLOAD_FAILED));
        assertError(() -> service.replacePostImage(postId, imageId, file(), userId), ErrorCode.IMAGE_UPLOAD_FAILED);
        assertThat(images.findById(imageId).orElseThrow().getImageUrl()).isEqualTo(OLD_URL);
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        verify(storage, never()).delete(anyString());
    }

    @Test
    void databaseFailureCleansNewFileAndKeepsOldImage() {
        when(storage.upload(any(), eq("posts"))).thenReturn(ImageUploadResult.of("x".repeat(501), "new-file"));
        assertThatThrownBy(() -> service.replacePostImage(postId, imageId, file(), userId))
                .isInstanceOf(RuntimeException.class);
        assertThat(images.findById(imageId).orElseThrow().getImageUrl()).isEqualTo(OLD_URL);
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        verify(storage).delete("new-file");
        verify(storage, never()).delete("old-file");
    }

    @Test
    void commitFailureRollsBackReplacementAndPendingRecord() {
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            service.replacePostImage(postId, imageId, file(), userId);
            failBeforeCommit();
        })).isInstanceOf(IllegalStateException.class);
        assertThat(images.findById(imageId).orElseThrow().getImageUrl()).isEqualTo(OLD_URL);
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        verify(storage).delete("new-file");
        verify(storage, never()).delete("old-file");
    }

    @Test
    void deletesOldFileOnlyAfterNewUrlIsCommittedInContent() {
        service.replacePostImage(postId, imageId, file(), userId);
        verify(storage, never()).delete(anyString());

        service.updatePost(postId, request(body(NEW_URL)), userId);

        assertThat(savedContent()).isEqualTo(body(NEW_URL));
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        verify(storage).delete("old-file");
        verify(storage, never()).delete("new-file");
    }

    @Test
    void failedContentSaveDoesNotDeleteOldFileAndCanBeRetried() {
        service.replacePostImage(postId, imageId, file(), userId);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            service.updatePost(postId, request(body(NEW_URL)), userId);
            failBeforeCommit();
        })).isInstanceOf(IllegalStateException.class);
        assertThat(savedContent()).isEqualTo(body(OLD_URL));
        assertThat(pending.findByPostIdOrderById(postId)).hasSize(1);
        verify(storage, never()).delete("old-file");

        service.updatePost(postId, request(body(NEW_URL)), userId);
        verify(storage).delete("old-file");
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
    }

    @Test
    void keepsOldFileWhileBodyStillReferencesIt() {
        service.replacePostImage(postId, imageId, file(), userId);
        service.updatePost(postId, request(body(OLD_URL) + body(NEW_URL)), userId);
        assertThat(pending.findByPostIdOrderById(postId)).hasSize(1);
        verify(storage, never()).delete("old-file");

        service.updatePost(postId, request(body(NEW_URL)), userId);
        verify(storage).delete("old-file");
    }

    @Test
    void repeatedReplacementsKeepAllOldFilesUntilLatestUrlIsSaved() {
        when(storage.upload(any(), eq("posts")))
                .thenReturn(ImageUploadResult.of("https://example.com/middle", "middle-file"))
                .thenReturn(ImageUploadResult.of(NEW_URL, "new-file"));
        service.replacePostImage(postId, imageId, file(), userId);
        service.replacePostImage(postId, imageId, file(), userId);
        assertThat(pending.findByPostIdOrderById(postId)).hasSize(2);
        verify(storage, never()).delete(anyString());

        service.updatePost(postId, request(body(NEW_URL)), userId);
        verify(storage).delete("old-file");
        verify(storage).delete("middle-file");
        verify(storage, never()).delete("new-file");
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
    }

    @Test
    void failedStorageDeletionRetainsRecordAndRetriesOnNextSave() {
        service.replacePostImage(postId, imageId, file(), userId);
        doThrow(new DeveryTimeException(ErrorCode.IMAGE_DELETE_FAILED)).doNothing().when(storage).delete("old-file");

        service.updatePost(postId, request(body(NEW_URL)), userId);
        assertThat(savedContent()).isEqualTo(body(NEW_URL));
        assertThat(pending.findByPostIdOrderById(postId)).hasSize(1);

        service.updatePost(postId, request(body(NEW_URL)), userId);
        verify(storage, times(2)).delete("old-file");
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
    }

    @Test
    void recognizesEscapedUrlsInJsonBody() {
        service.replacePostImage(postId, imageId, file(), userId);
        String json = "{\"images\":[\"https:\\/\\/example.com\\/old.png\",\"" + NEW_URL + "\"]}";
        service.updatePost(postId, request(json), userId);
        verify(storage, never()).delete("old-file");
        assertThat(pending.findByPostIdOrderById(postId)).hasSize(1);

        service.updatePost(postId, request("{\"images\":[\"" + NEW_URL + "\"]}"), userId);
        verify(storage).delete("old-file");
    }

    @Test
    void skipsDeletionWhenContentCannotBeParsedSafely() {
        service.replacePostImage(postId, imageId, file(), userId);
        service.updatePost(postId, request("{broken " + NEW_URL), userId);
        verify(storage, never()).delete("old-file");
        assertThat(pending.findByPostIdOrderById(postId)).hasSize(1);
    }

    private String savedContent() {
        return transaction.execute(status -> em.find(Post.class, postId).getContent());
    }

    private String body(String url) {
        return "<p>content</p><img src=\"" + url + "\">";
    }

    private PostUpdateRequest request(String content) {
        var request = new PostUpdateRequest();
        ReflectionTestUtils.setField(request, "title", "updated");
        ReflectionTestUtils.setField(request, "content", content);
        return request;
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "image.png", "image/png", new byte[]{1});
    }

    private void failBeforeCommit() {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void beforeCommit(boolean readOnly) {
                throw new IllegalStateException("simulated commit phase failure");
            }
        });
    }

    private void assertError(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(DeveryTimeException.class,
                failure -> assertThat(failure.getErrorCode()).isEqualTo(code));
    }
}
