package com.dms.deverytime.domain.post.service;

import com.dms.deverytime.domain.category.entity.Category;
import com.dms.deverytime.domain.category.repository.CategoryRepository;
import com.dms.deverytime.domain.comment.repository.CommentRepository;
import com.dms.deverytime.domain.post.dto.response.PostImageResponse;
import com.dms.deverytime.domain.post.entity.Post;
import com.dms.deverytime.domain.post.entity.PostCreateStatus;
import com.dms.deverytime.domain.post.entity.PostImage;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// 운영 DB/Cloudinary에 접근하지 않고 실제 JPA 트랜잭션과 잠금을 검증한다.
@SpringJUnitConfig(PostImageUploadTest.Config.class)
class PostImageUploadTest {
    @Autowired PostService service;
    @Autowired PostImageRepository images;
    @Autowired ImageStorageService storage;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired PlatformTransactionManager transactionManager;

    private TransactionTemplate transaction;
    private EntityManager em;
    private Long postId;
    private Long userId;
    private static final AtomicInteger sequence = new AtomicInteger();

    @BeforeEach
    void setUp() {
        reset(storage);
        transaction = new TransactionTemplate(transactionManager);
        em = SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
        transaction.executeWithoutResult(status -> {
            int n = sequence.incrementAndGet();
            User user = User.builder().schoolYear(2026).schoolNumber(String.valueOf(n))
                    .name("tester").email("user" + n + "@example.com")
                    .username("user" + n).passwordHash("test").build();
            Category category = new Category("category" + n);
            em.persist(user);
            em.persist(category);
            Post post = new Post(user, category, "title", "content", PostCreateStatus.PUBLISHED);
            em.persist(post);
            postId = post.getId();
            userId = user.getId();
        });
        AtomicInteger uploads = new AtomicInteger();
        when(storage.upload(any(), eq("posts"))).thenAnswer(invocation -> {
            int n = uploads.incrementAndGet();
            return ImageUploadResult.of("https://example.com/" + n, "public-" + n);
        });
    }

    @Test
    void uploadsThreeImagesInRequestOrderAndKeepsCommittedFiles() {
        List<PostImageResponse> response = service.uploadPostImages(postId,
                List.of(file(), file(), file()), userId);

        assertThat(response).extracting(PostImageResponse::getSortOrder).containsExactly(0, 1, 2);
        assertThat(response).extracting(PostImageResponse::getId).doesNotContainNull();
        assertThat(images.findByPostIdOrderBySortOrder(postId)).hasSize(3);
        verify(storage, never()).delete(anyString());
    }

    @Test
    void appendsAfterExistingMaximumOrder() {
        seedImage(4);
        seedImage(8);
        assertThat(service.uploadPostImages(postId, List.of(file()), userId).get(0).getSortOrder())
                .isEqualTo(9);
        assertThat(images.findByPostIdOrderBySortOrder(postId)).hasSize(3);
    }

    @Test
    void rejectsMoreThanThreeIncludingExistingImagesBeforeUploading() {
        seedImage(0);
        seedImage(1);
        assertError(() -> service.uploadPostImages(postId, List.of(file(), file()), userId),
                ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);
        verify(storage, never()).upload(any(), anyString());
        assertThat(images.findByPostIdOrderBySortOrder(postId)).hasSize(2);
    }

    @Test
    void rejectsMissingOrEmptyFiles() {
        assertError(() -> service.uploadPostImages(postId, null, userId), ErrorCode.INVALID_IMAGE_FILE);
        assertError(() -> service.uploadPostImages(postId, List.of(), userId), ErrorCode.INVALID_IMAGE_FILE);
        verify(storage, never()).upload(any(), anyString());
    }

    @Test
    void validatesEveryFileBeforeFirstUpload() {
        MockMultipartFile invalid = file();
        doThrow(new DeveryTimeException(ErrorCode.INVALID_IMAGE_FILE)).when(storage).validateImage(invalid);
        assertError(() -> service.uploadPostImages(postId, List.of(file(), invalid), userId),
                ErrorCode.INVALID_IMAGE_FILE);
        verify(storage, never()).upload(any(), anyString());
    }

    @Test
    void rejectsUnknownPostAndNonOwner() {
        assertError(() -> service.uploadPostImages(-1L, List.of(file()), userId), ErrorCode.POST_NOT_FOUND);
        assertError(() -> service.uploadPostImages(postId, List.of(file()), -1L), ErrorCode.FORBIDDEN);
        verify(storage, never()).upload(any(), anyString());
    }

    @Test
    void secondUploadFailureRollsBackRowsAndDeletesFirstFile() {
        when(storage.upload(any(), eq("posts")))
                .thenReturn(ImageUploadResult.of("https://example.com/first", "first"))
                .thenThrow(new DeveryTimeException(ErrorCode.IMAGE_UPLOAD_FAILED));
        assertError(() -> service.uploadPostImages(postId, List.of(file(), file()), userId),
                ErrorCode.IMAGE_UPLOAD_FAILED);
        assertThat(images.findByPostIdOrderBySortOrder(postId)).isEmpty();
        verify(storage).delete("first");
    }

    @Test
    void databaseInsertFailureDeletesUploadedFile() {
        when(storage.upload(any(), eq("posts")))
                .thenReturn(ImageUploadResult.of("x".repeat(501), "too-long-url"));
        assertThatThrownBy(() -> service.uploadPostImages(postId, List.of(file()), userId))
                .isInstanceOf(RuntimeException.class);
        assertThat(images.findByPostIdOrderBySortOrder(postId)).isEmpty();
        verify(storage).delete("too-long-url");
    }

    @Test
    void commitPhaseFailureAlsoCleansUploadedFilesAndContinuesAfterCleanupFailure() {
        doThrow(new DeveryTimeException(ErrorCode.IMAGE_DELETE_FAILED)).when(storage).delete("public-1");
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            service.uploadPostImages(postId, List.of(file(), file()), userId);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    throw new IllegalStateException("simulated commit phase failure");
                }
            });
        })).isInstanceOf(IllegalStateException.class);
        assertThat(images.findByPostIdOrderBySortOrder(postId)).isEmpty();
        verify(storage).delete("public-1");
        verify(storage).delete("public-2");
    }

    @Test
    void concurrentUploadsCannotExceedThreeImages() throws Exception {
        seedImage(0);
        seedImage(1);
        CountDownLatch firstUploadStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondRequestStarted = new CountDownLatch(1);
        when(storage.upload(any(), eq("posts"))).thenAnswer(invocation -> {
            firstUploadStarted.countDown();
            if (!releaseFirst.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("upload gate timed out");
            }
            return ImageUploadResult.of("https://example.com/new", "new");
        });
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> service.uploadPostImages(postId, List.of(file()), userId));
            assertThat(firstUploadStarted.await(5, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> {
                secondRequestStarted.countDown();
                try {
                    service.uploadPostImages(postId, List.of(file()), userId);
                    return (ErrorCode) null;
                } catch (DeveryTimeException failure) {
                    return failure.getErrorCode();
                }
            });
            assertThat(secondRequestStarted.await(5, TimeUnit.SECONDS)).isTrue();
            releaseFirst.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS)).hasSize(1);
            assertThat(second.get(5, TimeUnit.SECONDS)).isEqualTo(ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);
            assertThat(images.findByPostIdOrderBySortOrder(postId)).hasSize(3);
            verify(storage, times(1)).upload(any(), eq("posts"));
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private void seedImage(int order) {
        transaction.executeWithoutResult(status -> em.persist(new PostImage(
                em.getReference(Post.class, postId), "https://example.com/existing", "existing", order)));
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("files", "image.png", "image/png", new byte[]{1});
    }

    private void assertError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(DeveryTimeException.class,
                failure -> assertThat(failure.getErrorCode()).isEqualTo(expected));
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = PostRepository.class)
    static class Config {
        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource(
                    "jdbc:h2:mem:post-images;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "sa", "");
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.dms.deverytime.domain");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of(
                    "hibernate.hbm2ddl.auto", "create-drop",
                    "hibernate.physical_naming_strategy",
                    "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl"));
            return factory;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }

        @Bean
        ImageStorageService imageStorageService() {
            return mock(ImageStorageService.class);
        }

        @Bean
        PostImageCleanupService cleanupService(PostRepository posts, PostImageRepository images,
                                               PostImageCleanupRepository pending, ImageStorageService storage) {
            return new PostImageCleanupService(posts, images, pending, storage);
        }

        @Bean
        PostService postService(PostRepository posts, PostImageRepository images, ImageStorageService storage,
                                PostImageCleanupRepository pending, PostImageCleanupService cleanup) {
            return new PostService(posts, mock(UserRepository.class), mock(CategoryRepository.class),
                    images, mock(PostViewLogRepository.class), mock(PostLikeRepository.class),
                    storage, mock(CommentRepository.class), pending, cleanup);
        }
    }
}
