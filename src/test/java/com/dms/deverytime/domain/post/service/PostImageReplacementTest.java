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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(PostImageUploadTest.Config.class)
class PostImageReplacementTest {
    @Autowired PostService service;
    @Autowired PostImageRepository images;
    @Autowired PostImageCleanupRepository pending;
    @Autowired PostImageCleanupService cleanupService;
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
        when(storage.deleteUploadCandidate(anyString())).thenReturn(true);
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
        when(storage.newPostImagePublicId()).thenReturn("new-file");
        when(storage.uploadReservedPostImage(any(), anyString())).thenReturn(ImageUploadResult.of(NEW_URL, "new-file"));
    }

    @Test
    void committedUploadCandidateCannotDeleteImageStillReferencedAfterReplacement() {
        when(storage.newPostImagePublicId()).thenReturn("preserved-image-a", "replacement-image-b");
        when(storage.uploadReservedPostImage(any(), anyString()))
                .thenReturn(ImageUploadResult.of(NEW_URL, "preserved-image-a"))
                .thenReturn(ImageUploadResult.of("https://example.com/next.png", "replacement-image-b"));

        transaction.executeWithoutResult(status -> {
            service.replacePostImage(postId, imageId, file(), userId);
            assertThat(pending.findAll()).filteredOn(record -> record.getPublicId().equals("preserved-image-a"))
                    .singleElement().satisfies(record -> assertThat(record.isCompleted()).isTrue());
        });
        transaction.executeWithoutResult(status ->
                assertThat(pending.findAll()).filteredOn(record -> record.getPublicId().equals("preserved-image-a"))
                        .singleElement().satisfies(record -> assertThat(record.isCompleted()).isTrue()));
        service.updatePost(postId, request(body(NEW_URL)), userId);
        service.replacePostImage(postId, imageId, file(), userId);
        cleanupService.cleanupAfterContentSaved(postId);

        assertThat(savedContent()).isEqualTo(body(NEW_URL));
        verify(storage, never()).delete("preserved-image-a");
    }

    @Test
    void replacementPreservesUnsavedEditorImageUntilContentIsSaved() {
        service.updatePost(postId, request("text only"), userId);
        service.replacePostImage(postId, imageId, file(), userId);

        verify(storage, never()).delete("old-file");
        assertThat(pending.findByPostIdOrderById(postId)).hasSize(1);
        service.updatePost(postId, request(body(NEW_URL)), userId);
        verify(storage).delete("old-file");
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
    }

    @Test
    void oldBodyCannotRestoreDeletedFileUrlAfterReplacement() {
        service.replacePostImage(postId, imageId, file(), userId);
        service.updatePost(postId, request(body(NEW_URL)), userId);
        assertError(() -> service.updatePost(postId, request(body(OLD_URL)), userId),
                ErrorCode.POST_IMAGE_CONTENT_CONFLICT);
        assertThat(savedContent()).isEqualTo(body(NEW_URL));
        assertThat(pending.findByPostIdAndRetiredTrue(postId)).hasSize(1);
        assertThat(pending.findByPostIdAndRetiredTrue(postId).get(0).isCompleted()).isTrue();
        verify(storage, times(1)).delete("old-file");
    }

    @Test
    void markdownEscapedImageReferencePreservesReplacedFile() {
        service.replacePostImage(postId, imageId, file(), userId);
        String content = "![사진](https://example.com/old\\.png)";

        service.updatePost(postId, request(content), userId);

        assertThat(savedContent()).isEqualTo(content);
        verify(storage, never()).delete("old-file");
        assertThat(pending.findByPostIdOrderById(postId)).hasSize(1);
    }

    @Test
    void html5NamedEntityReferencePreservesReplacedFile() {
        service.replacePostImage(postId, imageId, file(), userId);
        service.updatePost(postId, request("![사진](https://example.com/old&period;png)"), userId);
        verify(storage, never()).delete("old-file");
    }

    @Test
    void html5NamedEntityReferenceBlocksDeletion() {
        assertError(() -> service.updatePost(postId,
                deletionRequest("![사진](https://example.com/old&period;png)", imageId), userId),
                ErrorCode.VALIDATION_ERROR);
        verify(storage, never()).delete("old-file");
    }

    @Test
    void percentEncodedUrlPreservesReplacedFile() {
        service.replacePostImage(postId, imageId, file(), userId);
        service.updatePost(postId, request("![사진](https://example.com/old%2Epng)"), userId);
        verify(storage, never()).delete("old-file");
    }

    @Test
    void markdownEntityWithoutSemicolonDoesNotBlockDeletion() {
        service.updatePost(postId, deletionRequest("![사진](https://example.com/old&#46png)", imageId), userId);
        assertThat(images.findById(imageId)).isEmpty();
    }

    @Test
    void htmlEntityWithoutSemicolonBlocksDeletion() {
        assertError(() -> service.updatePost(postId,
                deletionRequest("<img src=\"https://example.com/old&#46png\">", imageId), userId),
                ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void markdownEscapedImageReferenceBlocksExplicitDeletion() {
        String content = "![사진](https://example.com/old\\.png)";

        assertError(() -> service.updatePost(postId, deletionRequest(content, imageId), userId),
                ErrorCode.VALIDATION_ERROR);
        assertThat(images.findById(imageId)).isPresent();
        verify(storage, never()).delete("old-file");
    }

    @Test
    void oldBodyCannotRestoreDeletedPhotoUrlWithEscapedJson() {
        service.updatePost(postId, deletionRequest("text only", imageId), userId);
        String oldJson = "{\"images\":[\"https:\\/\\/example.com\\/old.png\"]}";
        assertError(() -> service.updatePost(postId, request(oldJson), userId),
                ErrorCode.POST_IMAGE_CONTENT_CONFLICT);
        assertThat(savedContent()).isEqualTo("text only");
    }

    @Test
    void retiredUrlCannotReturnEvenIfExternalDeletionFailed() {
        doThrow(new DeveryTimeException(ErrorCode.IMAGE_DELETE_FAILED)).when(storage).delete("old-file");
        service.updatePost(postId, deletionRequest("text only", imageId), userId);
        assertError(() -> service.updatePost(postId, request(body(OLD_URL)), userId),
                ErrorCode.POST_IMAGE_CONTENT_CONFLICT);
        assertThat(pending.findByPostIdOrderById(postId)).hasSize(1);
        assertThat(savedContent()).isEqualTo("text only");
    }

    @Test
    void databaseFailureAfterExternalDeleteStillBlocksStaleBodyAndRetriesCleanup() {
        doAnswer(invocation -> {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void beforeCommit(boolean readOnly) {
                    throw new IllegalStateException("cleanup commit failure");
                }
            });
            return null;
        }).doNothing().when(storage).delete("old-file");
        service.updatePost(postId, deletionRequest("text only", imageId), userId);
        assertThat(pending.findByPostIdOrderById(postId)).hasSize(1);
        assertError(() -> service.updatePost(postId, request(body(OLD_URL)), userId),
                ErrorCode.POST_IMAGE_CONTENT_CONFLICT);
        service.updatePost(postId, request("text only"), userId);
        verify(storage, times(2)).delete("old-file");
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"[공지] 사진을 삭제했습니다.", "{알림} 사진을 삭제했습니다.", "\"안내 사진을 삭제했습니다."})
    void plainTextJsonPrefixDoesNotRejectPhotoDeletion(String content) {
        service.updatePost(postId, deletionRequest(content, imageId), userId);
        assertThat(savedContent()).isEqualTo(content);
        assertThat(images.findById(imageId)).isEmpty();
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        verify(storage).delete("old-file");
    }

    @Test
    void malformedPlainTextStillRejectsActualDeletedImageUrl() {
        assertError(() -> service.updatePost(postId, deletionRequest("[공지] " + OLD_URL, imageId), userId),
                ErrorCode.VALIDATION_ERROR);
        assertThat(images.findById(imageId)).isPresent();
        verify(storage, never()).delete(anyString());
    }

    @Test
    void concurrentOlderViewCannotOverwriteNewContentOrRestoreRemovedImage() throws Exception {
        service.replacePostImage(postId, imageId, file(), userId);
        var readStarted = new java.util.concurrent.CountDownLatch(1);
        var finishRead = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var viewing = executor.submit(() -> transaction.executeWithoutResult(status -> {
                Post stalePost = em.find(Post.class, postId);
                assertThat(stalePost.getContent()).isEqualTo(body(OLD_URL));
                readStarted.countDown();
                try {
                    if (!finishRead.await(10, java.util.concurrent.TimeUnit.SECONDS)) {
                        throw new IllegalStateException("view gate timeout");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
                stalePost.increaseViewCount();
            }));
            assertThat(readStarted.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            service.updatePost(postId, request(body(NEW_URL)), userId);
            finishRead.countDown();
            viewing.get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(savedContent()).isEqualTo(body(NEW_URL));
            String savedTitle = transaction.execute(status -> em.find(Post.class, postId).getTitle());
            Integer views = transaction.execute(status -> em.find(Post.class, postId).getViewCount());
            assertThat(savedTitle).isEqualTo("updated");
            assertThat(views).isEqualTo(1);
            verify(storage).delete("old-file");
        } finally {
            finishRead.countDown();
            executor.shutdownNow();
        }
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
        verify(storage, never()).uploadReservedPostImage(any(), anyString());
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
        verify(storage, never()).uploadReservedPostImage(any(), anyString());
    }

    @Test
    void invalidFileAndUploadFailureKeepOldImage() {
        when(storage.newPostImagePublicId()).thenReturn("failed-upload-replacement-unique");
        doThrow(new DeveryTimeException(ErrorCode.INVALID_IMAGE_FILE)).when(storage).validateImage(null);
        assertError(() -> service.replacePostImage(postId, imageId, null, userId), ErrorCode.INVALID_IMAGE_FILE);
        when(storage.uploadReservedPostImage(any(), anyString())).thenThrow(new DeveryTimeException(ErrorCode.IMAGE_UPLOAD_FAILED));
        assertError(() -> service.replacePostImage(postId, imageId, file(), userId), ErrorCode.IMAGE_UPLOAD_FAILED);
        assertThat(images.findById(imageId).orElseThrow().getImageUrl()).isEqualTo(OLD_URL);
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        verify(storage).deleteUploadCandidate("failed-upload-replacement-unique");
    }

    @Test
    void databaseFailureCleansNewFileAndKeepsOldImage() {
        when(storage.newPostImagePublicId()).thenReturn("failed-insert-file");
        when(storage.uploadReservedPostImage(any(), anyString())).thenReturn(ImageUploadResult.of("x".repeat(501), "failed-insert-file"));
        assertThatThrownBy(() -> service.replacePostImage(postId, imageId, file(), userId))
                .isInstanceOf(RuntimeException.class);
        assertThat(images.findById(imageId).orElseThrow().getImageUrl()).isEqualTo(OLD_URL);
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        verify(storage).deleteUploadCandidate("failed-insert-file");
        verify(storage, never()).delete("old-file");
    }

    @Test
    void commitFailureRollsBackReplacementAndPendingRecord() {
        when(storage.newPostImagePublicId()).thenReturn("failed-commit-file");
        when(storage.uploadReservedPostImage(any(), anyString())).thenReturn(ImageUploadResult.of(NEW_URL, "failed-commit-file"));
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            service.replacePostImage(postId, imageId, file(), userId);
            failBeforeCommit();
        })).isInstanceOf(IllegalStateException.class);
        assertThat(images.findById(imageId).orElseThrow().getImageUrl()).isEqualTo(OLD_URL);
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        verify(storage).deleteUploadCandidate("failed-commit-file");
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

    @ParameterizedTest
    @ValueSource(strings = {"text only", "[문서](https://example.com/docs)\n\n**사진 없는 본문**"})
    void replacementWithoutImageUrlsCleansOldFileOnlyAfterContentCommit(String content) {
        service.replacePostImage(postId, imageId, file(), userId);

        transaction.executeWithoutResult(status -> {
            service.updatePost(postId, request(content), userId);
            verify(storage, never()).delete("old-file");
        });

        assertThat(savedContent()).isEqualTo(content);
        assertThat(images.findById(imageId).orElseThrow().getImageUrl()).isEqualTo(NEW_URL);
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        assertThat(pending.findByPostIdAndRetiredTrue(postId)).hasSize(1)
                .allMatch(cleanup -> cleanup.isCompleted());
        verify(storage).delete("old-file");
        verify(storage, never()).delete("new-file");
        assertError(() -> service.updatePost(postId, request(body(OLD_URL)), userId),
                ErrorCode.POST_IMAGE_CONTENT_CONFLICT);
    }

    @Test
    void repeatedReplacementsWithoutImageUrlsCleanAllPreviousFiles() {
        when(storage.newPostImagePublicId()).thenReturn("middle-file", "new-file");
        when(storage.uploadReservedPostImage(any(), anyString()))
                .thenReturn(ImageUploadResult.of("https://example.com/middle", "middle-file"))
                .thenReturn(ImageUploadResult.of(NEW_URL, "new-file"));
        service.replacePostImage(postId, imageId, file(), userId);
        service.replacePostImage(postId, imageId, file(), userId);

        service.updatePost(postId, request("text only"), userId);

        assertThat(savedContent()).isEqualTo("text only");
        assertThat(images.findById(imageId).orElseThrow().getImageUrl()).isEqualTo(NEW_URL);
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        verify(storage).delete("old-file");
        verify(storage).delete("middle-file");
        verify(storage, never()).delete("new-file");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://example.com/old.png",
            "https://example.com/old\\u002epng",
            "https:\\/\\/example.com\\/old.png",
            "https:&#47;&#47;example.com&#47;old.png"
    })
    void oldImageUrlInJsonKeyIsPreservedAndValidated(String keyUrl) {
        service.replacePostImage(postId, imageId, file(), userId);
        String content = "{\"![이전](" + keyUrl + ")\":\"![새 사진](" + NEW_URL + ")\"}";

        service.updatePost(postId, request(content), userId);

        assertThat(savedContent()).isEqualTo(content);
        assertThat(pending.findByPostIdOrderById(postId)).hasSize(1);
        verify(storage, never()).delete("old-file");
        assertError(() -> service.updatePost(postId, deletionRequest(content, imageId), userId),
                ErrorCode.VALIDATION_ERROR);

        service.updatePost(postId, request(body(NEW_URL)), userId);
        verify(storage).delete("old-file");
        assertError(() -> service.updatePost(postId, request(content), userId),
                ErrorCode.POST_IMAGE_CONTENT_CONFLICT);
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
        when(storage.newPostImagePublicId()).thenReturn("middle-file", "new-file");
        when(storage.uploadReservedPostImage(any(), anyString()))
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
    void plainTextStartingWithBraceStillCleansUnreferencedOldFile() {
        service.replacePostImage(postId, imageId, file(), userId);
        service.updatePost(postId, request("{broken " + NEW_URL), userId);
        verify(storage).delete("old-file");
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"## 제목\n\n**굵게**\n\n", "[문서](https://example.com/docs)\n\n", "> 인용문\n\n"})
    void markdownReplacementPreservesBodyAndCleansOldFile(String prefix) {
        service.replacePostImage(postId, imageId, file(), userId);
        String markdown = prefix + "![사진](" + NEW_URL + ")";
        service.updatePost(postId, request(markdown), userId);
        assertThat(savedContent()).isEqualTo(markdown);
        verify(storage).delete("old-file");
        verify(storage, never()).delete("new-file");
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        assertError(() -> service.updatePost(postId, request(prefix + "![이전 사진](" + OLD_URL + ")"), userId),
                ErrorCode.POST_IMAGE_CONTENT_CONFLICT);
    }

    @Test
    void markdownOldImageReferenceKeepsFileAndRejectsExplicitDeletion() {
        service.replacePostImage(postId, imageId, file(), userId);
        String markdown = "[문서](https://example.com/docs)\n\n![이전](" + OLD_URL + ")\n![새 사진](" + NEW_URL + ")";
        service.updatePost(postId, request(markdown), userId);
        assertThat(savedContent()).isEqualTo(markdown);
        verify(storage, never()).delete("old-file");
        assertThat(pending.findByPostIdOrderById(postId)).hasSize(1);
        assertError(() -> service.updatePost(postId, deletionRequest(markdown, imageId), userId),
                ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void markdownDeletionPreservesRemainingMarkdownExactly() {
        String markdown = "[문서](https://example.com/docs)\n\n## 제목\n\n**본문**과 *강조*, <u>밑줄</u>";
        service.updatePost(postId, deletionRequest(markdown, imageId), userId);
        assertThat(savedContent()).isEqualTo(markdown);
        assertThat(images.findById(imageId)).isEmpty();
        verify(storage).delete("old-file");
    }

    @Test
    void deletesOneOfThreeAfterCommitAndPreservesRemainingOrder() {
        transaction.executeWithoutResult(status -> {
            Post post = em.getReference(Post.class, postId);
            em.persist(new PostImage(post, "https://example.com/second", "second", 5));
            em.persist(new PostImage(post, "https://example.com/third", "third", 6));
        });
        transaction.executeWithoutResult(status -> {
            service.updatePost(postId, deletionRequest("updated content", imageId), userId);
            verify(storage, never()).delete(anyString());
        });
        assertThat(images.findByPostIdOrderBySortOrder(postId)).extracting(PostImage::getSortOrder)
                .containsExactly(5, 6);
        assertThat(savedContent()).isEqualTo("updated content");
        verify(storage).delete("old-file");
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        service.uploadPostImages(postId, List.of(file()), userId);
        assertThat(images.findByPostIdOrderBySortOrder(postId)).hasSize(3);
    }

    @Test
    void deletesLastPhotoButKeepsPostAndAcceptsDuplicateIdsOnce() {
        service.updatePost(postId, deletionRequest("text only", imageId, imageId), userId);
        assertThat(images.findByPostIdOrderBySortOrder(postId)).isEmpty();
        assertThat(savedContent()).isEqualTo("text only");
        verify(storage, times(1)).delete("old-file");
    }

    @Test
    void omittedOrEmptyDeletionListPreservesPhotos() {
        service.updatePost(postId, request("first update"), userId);
        service.updatePost(postId, deletionRequest("second update"), userId);
        assertThat(images.findById(imageId)).isPresent();
        verify(storage, never()).delete(anyString());
    }

    @Test
    void rejectsInvalidOrForeignIdWithoutPartialUpdate() {
        Long foreignId = transaction.execute(status -> {
            Post original = em.find(Post.class, postId);
            Post other = new Post(original.getUser(), original.getCategory(), "other", "content",
                    PostCreateStatus.PUBLISHED);
            em.persist(other);
            PostImage foreign = new PostImage(other, NEW_URL, "foreign", 0);
            em.persist(foreign);
            return foreign.getId();
        });
        assertError(() -> service.updatePost(postId, deletionRequest("changed", imageId, foreignId), userId),
                ErrorCode.POST_IMAGE_NOT_FOUND);
        assertError(() -> service.updatePost(postId, deletionRequest("changed", imageId, -1L), userId),
                ErrorCode.POST_IMAGE_NOT_FOUND);
        assertError(() -> service.updatePost(postId, deletionRequest("changed", imageId), -1L),
                ErrorCode.FORBIDDEN);
        assertThat(savedContent()).isEqualTo(body(OLD_URL));
        assertThat(images.findById(imageId)).isPresent();
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        verify(storage, never()).delete(anyString());
    }

    @Test
    void rejectsReferencedImageIncludingEscapedJsonAndPreviousReplacementUrl() {
        assertError(() -> service.updatePost(postId, deletionRequest(body(OLD_URL), imageId), userId),
                ErrorCode.VALIDATION_ERROR);
        String json = "{\"images\":[\"https:\\/\\/example.com\\/old.png\"]}";
        assertError(() -> service.updatePost(postId, deletionRequest(json, imageId), userId),
                ErrorCode.VALIDATION_ERROR);
        service.replacePostImage(postId, imageId, file(), userId);
        assertError(() -> service.updatePost(postId, deletionRequest(body(OLD_URL), imageId), userId),
                ErrorCode.VALIDATION_ERROR);
        verify(storage, never()).delete(anyString());
    }

    @Test
    void rollbackRestoresPhotoContentAndCleanupRecords() {
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            service.updatePost(postId, deletionRequest("text only", imageId), userId);
            failBeforeCommit();
        })).isInstanceOf(IllegalStateException.class);
        assertThat(savedContent()).isEqualTo(body(OLD_URL));
        assertThat(images.findById(imageId)).isPresent();
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        verify(storage, never()).delete(anyString());
    }

    @Test
    void failedDeletedPhotoCleanupIsRetriedOnNextSave() {
        doThrow(new DeveryTimeException(ErrorCode.IMAGE_DELETE_FAILED)).doNothing()
                .when(storage).delete("old-file");
        service.updatePost(postId, deletionRequest("text only", imageId), userId);
        assertThat(images.findById(imageId)).isEmpty();
        assertThat(pending.findByPostIdOrderById(postId)).hasSize(1);
        service.updatePost(postId, request("next save"), userId);
        verify(storage, times(2)).delete("old-file");
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
    }

    @Test
    void deletingPostAlsoRemovesCurrentImageFromStorage() {
        service.deletePost(postId, userId);

        assertThat(images.findById(imageId)).isEmpty();
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        verify(storage).delete("old-file");
    }

    @Test
    void deletingPostRetriesCurrentImageIfStorageDeletionFails() {
        doThrow(new DeveryTimeException(ErrorCode.IMAGE_DELETE_FAILED)).doNothing()
                .when(storage).delete("old-file");
        service.deletePost(postId, userId);

        assertThat(images.findById(imageId)).isEmpty();
        assertThat(pending.findByPostIdOrderById(postId)).hasSize(1);
        PostImageCleanupRepository retryQueue = mock(PostImageCleanupRepository.class);
        when(retryQueue.findPostIdsWithRetryableCleanup(any())).thenReturn(List.of(postId));
        new PostImageCleanupRetry(retryQueue, cleanupService).retryPendingCleanup();

        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        verify(storage, times(2)).delete("old-file");
    }

    @Test
    void rolledBackPostDeletionKeepsCurrentImage() {
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            service.deletePost(postId, userId);
            failBeforeCommit();
        })).isInstanceOf(IllegalStateException.class);

        assertThat(images.findById(imageId)).isPresent();
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        verify(storage, never()).delete("old-file");
    }

    @Test
    void failedCleanupForDeletedPostIsRetriedWithoutAnotherSave() {
        service.replacePostImage(postId, imageId, file(), userId);
        doThrow(new DeveryTimeException(ErrorCode.IMAGE_DELETE_FAILED)).doNothing()
                .when(storage).delete("old-file");

        service.deletePost(postId, userId);
        assertThat(pending.findByPostIdOrderById(postId)).hasSize(1);
        PostImageCleanupRepository retryQueue = mock(PostImageCleanupRepository.class);
        when(retryQueue.findPostIdsWithRetryableCleanup(any())).thenReturn(List.of(postId));
        new PostImageCleanupRetry(retryQueue, cleanupService).retryPendingCleanup();

        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        verify(storage, times(2)).delete("old-file");
    }

    @Test
    void deletionAfterReplacementCleansBothVersions() {
        service.replacePostImage(postId, imageId, file(), userId);
        service.updatePost(postId, deletionRequest("text only", imageId), userId);
        verify(storage).delete("old-file");
        verify(storage).delete("new-file");
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
    }

    private PostUpdateRequest deletionRequest(String content, Long... ids) {
        PostUpdateRequest request = request(content);
        ReflectionTestUtils.setField(request, "deleteImageIds", List.of(ids));
        return request;
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
