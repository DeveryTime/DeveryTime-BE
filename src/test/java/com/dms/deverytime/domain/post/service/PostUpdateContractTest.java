package com.dms.deverytime.domain.post.service;

import com.dms.deverytime.domain.category.entity.Category;
import com.dms.deverytime.domain.category.repository.CategoryRepository;
import com.dms.deverytime.domain.comment.repository.CommentRepository;
import com.dms.deverytime.domain.post.dto.request.PostUpdateRequest;
import com.dms.deverytime.domain.post.entity.*;
import com.dms.deverytime.domain.post.enums.PostCreateStatus;
import com.dms.deverytime.domain.post.repository.*;
import com.dms.deverytime.domain.postlike.repository.PostLikeRepository;
import com.dms.deverytime.domain.user.entity.User;
import com.dms.deverytime.domain.user.repository.UserRepository;
import com.dms.deverytime.global.cloudinary.service.ImageStorageService;
import com.dms.deverytime.global.exception.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(PostUpdateContractTest.Config.class)
class PostUpdateContractTest {
    @Autowired PostService service;
    @Autowired PostRepository posts;
    @Autowired PostImageRepository images;
    @Autowired PostImageCleanupRepository pending;
    @Autowired ImageStorageService storage;
    @Autowired EntityManagerFactory factory;
    @Autowired PlatformTransactionManager manager;
    private static final AtomicInteger sequence = new AtomicInteger(3000);
    private TransactionTemplate tx;
    private EntityManager em;
    private Long postId, userId, originalCategoryId, newCategoryId;
    private String keyword;

    @BeforeEach
    void setup() {
        reset(storage);
        tx = new TransactionTemplate(manager);
        em = SharedEntityManagerCreator.createSharedEntityManager(factory);
        tx.executeWithoutResult(s -> {
            int n = sequence.incrementAndGet();
            keyword = "contract" + n;
            User user = User.builder().schoolYear(2026).schoolNumber(String.valueOf(n)).name("tester")
                    .email(keyword + "@example.com").username("c" + n).passwordHash("test").build();
            Category first = new Category(keyword), second = new Category(keyword + "new");
            em.persist(user); em.persist(first); em.persist(second);
            Post post = new Post(user, first, keyword, "original", PostCreateStatus.DRAFT);
            em.persist(post);
            postId = post.getId(); userId = user.getId(); originalCategoryId = first.getId();
            newCategoryId = second.getId();
        });
    }

    @Test
    void delayedFirstViewCannotRepublishDraftOrOverwriteEditedContent() throws Exception {
        service.updatePost(postId, request(null, PostCreateStatus.PUBLISHED), userId);
        User viewer = tx.execute(s -> em.find(User.class, userId));
        var users = mock(UserRepository.class);
        var views = mock(PostViewLogRepository.class);
        var readStarted = new java.util.concurrent.CountDownLatch(1);
        var finishRead = new java.util.concurrent.CountDownLatch(1);
        when(users.findById(userId)).thenReturn(java.util.Optional.of(viewer));
        when(views.existsByPostIdAndUserId(postId, userId)).thenAnswer(invocation -> {
            readStarted.countDown();
            if (!finishRead.await(10, java.util.concurrent.TimeUnit.SECONDS)) {
                throw new IllegalStateException("view gate timeout");
            }
            return false;
        });
        var viewingService = new PostService(posts, users, mock(CategoryRepository.class), images, views,
                mock(PostLikeRepository.class), mock(ImageStorageService.class), mock(CommentRepository.class),
                mock(PostImageCleanupRepository.class), mock(PostImageCleanupService.class));
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var viewing = executor.submit(() -> tx.executeWithoutResult(s ->
                    viewingService.getPostDetail(postId, userId)));
            assertThat(readStarted.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var edit = new PostUpdateRequest(newCategoryId, PostCreateStatus.DRAFT, "private title",
                    "private content", null);
            service.updatePost(postId, edit, userId);
            finishRead.countDown();
            viewing.get(10, java.util.concurrent.TimeUnit.SECONDS);
            tx.executeWithoutResult(s -> {
                Post saved = em.find(Post.class, postId);
                assertThat(saved.getStatus()).isEqualTo(PostCreateStatus.DRAFT);
                assertThat(saved.getTitle()).isEqualTo("private title");
                assertThat(saved.getContent()).isEqualTo("private content");
                assertThat(saved.getCategory().getId()).isEqualTo(newCategoryId);
                assertThat(saved.getViewCount()).isEqualTo(1);
            });
            assertError(() -> service.getPostDetail(postId, -1L), ErrorCode.POST_NOT_FOUND);
            verify(views).save(any(PostViewLog.class));
        } finally {
            finishRead.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void publishesChangesCategoryAndReturnsPersistedTimestamp() {
        var response = service.updatePost(postId, request(newCategoryId, PostCreateStatus.PUBLISHED), userId);
        tx.executeWithoutResult(s -> {
            Post saved = em.find(Post.class, postId);
            assertThat(saved.getCategory().getId()).isEqualTo(newCategoryId);
            assertThat(saved.getStatus()).isEqualTo(PostCreateStatus.PUBLISHED);
            assertThat(saved.getTitle()).isEqualTo("updated");
            assertThat(saved.getContent()).isEqualTo("updated content");
            assertThat(response.id()).isEqualTo(postId);
            assertThat(response.title()).isEqualTo(saved.getTitle());
            assertThat(response.status()).isEqualTo(saved.getStatus());
            assertThat(response.updatedAt()).isNotNull().isEqualTo(saved.getUpdatedAt());
        });
    }

    @Test
    void publishesDraftChangesCategoryAndDeletesPhotoInOneSave() {
        Long imageId = addPhoto();
        String markdown = "## 제목\n\n**사진을 제거한 본문**";
        var request = new PostUpdateRequest(newCategoryId, PostCreateStatus.PUBLISHED, "updated", markdown,
                List.of(imageId));

        var response = service.updatePost(postId, request, userId);

        tx.executeWithoutResult(s -> {
            Post saved = em.find(Post.class, postId);
            assertThat(saved.getCategory().getId()).isEqualTo(newCategoryId);
            assertThat(saved.getStatus()).isEqualTo(PostCreateStatus.PUBLISHED);
            assertThat(saved.getContent()).isEqualTo(markdown);
            assertThat(response.id()).isEqualTo(postId);
            assertThat(response.status()).isEqualTo(saved.getStatus());
            assertThat(response.updatedAt()).isNotNull().isEqualTo(saved.getUpdatedAt());
        });
        assertThat(images.findById(imageId)).isEmpty();
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        verify(storage).delete("combined-photo");
    }

    @Test
    void invalidCategoryCannotPartiallyDeletePhotoOrPublishDraft() {
        Long imageId = addPhoto();

        assertError(() -> service.updatePost(postId,
                deletionRequest(-999L, PostCreateStatus.PUBLISHED, imageId), userId),
                ErrorCode.CATEGORY_NOT_FOUND);

        assertOriginal();
        assertThat(images.findById(imageId)).isPresent();
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        verify(storage, never()).delete(anyString());
    }

    @Test
    void rollbackRestoresPhotoCategoryAndDraftWithoutDeletingFile() {
        Long imageId = addPhoto();

        tx.executeWithoutResult(s -> {
            service.updatePost(postId, deletionRequest(newCategoryId, PostCreateStatus.PUBLISHED, imageId), userId);
            s.setRollbackOnly();
        });

        assertOriginal();
        assertThat(images.findById(imageId)).isPresent();
        assertThat(pending.findByPostIdOrderById(postId)).isEmpty();
        verify(storage, never()).delete(anyString());
    }

    @Test
    void omittedCategoryAndStatusPreserveOldClientBehavior() {
        var first = service.updatePost(postId, request(null, null), userId);
        var second = service.updatePost(postId, request(null, null), userId);
        tx.executeWithoutResult(s -> {
            Post saved = em.find(Post.class, postId);
            assertThat(saved.getCategory().getId()).isEqualTo(originalCategoryId);
            assertThat(saved.getStatus()).isEqualTo(PostCreateStatus.DRAFT);
            assertThat(second.updatedAt()).isEqualTo(saved.getUpdatedAt());
        });
        assertThat(first.updatedAt()).isNotNull();
        assertThat(second.updatedAt()).isAfterOrEqualTo(first.updatedAt());
    }

    @Test
    void unknownCategoryNonOwnerAndUnknownPostCannotPartiallyUpdate() {
        assertError(() -> service.updatePost(postId, request(-999L, PostCreateStatus.PUBLISHED), userId),
                ErrorCode.CATEGORY_NOT_FOUND);
        assertError(() -> service.updatePost(postId, request(newCategoryId, PostCreateStatus.PUBLISHED), -1L),
                ErrorCode.FORBIDDEN);
        assertError(() -> service.updatePost(-1L, request(newCategoryId, PostCreateStatus.PUBLISHED), userId),
                ErrorCode.POST_NOT_FOUND);
        assertOriginal();
    }

    @Test
    void outerRollbackRestoresCategoryStatusAndContent() {
        tx.executeWithoutResult(s -> {
            service.updatePost(postId, request(newCategoryId, PostCreateStatus.PUBLISHED), userId);
            s.setRollbackOnly();
        });
        assertOriginal();
    }

    @Test
    void publicListsAndSearchExcludeDraftsWhileMyPostsKeepsThem() {
        var pageable = PageRequest.of(0, 1000);
        assertThat(posts.findAllWithCategory(pageable)).extracting(Post::getStatus)
                .containsOnly(PostCreateStatus.PUBLISHED);
        assertThat(posts.findByCategoryIdWithCategory(originalCategoryId, pageable)).isEmpty();
        assertThat(posts.findByTitleContainingWithCategory(keyword, pageable)).isEmpty();
        assertThat(posts.findAllByUser_Id(userId, pageable)).extracting(Post::getId).containsExactly(postId);
        var request = new PostUpdateRequest(null, PostCreateStatus.PUBLISHED, keyword, "updated content", null);
        service.updatePost(postId, request, userId);
        assertThat(posts.findByCategoryIdWithCategory(originalCategoryId, pageable)).extracting(Post::getId)
                .containsExactly(postId);
        assertThat(posts.findByTitleContainingWithCategory(keyword, pageable)).extracting(Post::getId)
                .containsExactly(postId);
        assertThat(posts.findAllByUser_Id(userId, pageable)).extracting(Post::getId).containsExactly(postId);
    }

    private PostUpdateRequest request(Long categoryId, PostCreateStatus status) {
        return new PostUpdateRequest(categoryId, status, "updated", "updated content", null);
    }

    private PostUpdateRequest deletionRequest(Long categoryId, PostCreateStatus status, Long imageId) {
        return new PostUpdateRequest(categoryId, status, "updated", "updated content", List.of(imageId));
    }

    private Long addPhoto() {
        return tx.execute(s -> {
            PostImage image = new PostImage(em.getReference(Post.class, postId),
                    "https://example.com/combined.png", "combined-photo", 0);
            em.persist(image);
            return image.getId();
        });
    }

    private void assertOriginal() {
        tx.executeWithoutResult(s -> {
            Post saved = em.find(Post.class, postId);
            assertThat(saved.getTitle()).isEqualTo(keyword);
            assertThat(saved.getContent()).isEqualTo("original");
            assertThat(saved.getStatus()).isEqualTo(PostCreateStatus.DRAFT);
            assertThat(saved.getCategory().getId()).isEqualTo(originalCategoryId);
        });
    }

    private void assertError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(DeveryTimeException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = {PostRepository.class, CategoryRepository.class})
    static class Config {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:post-contract;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.dms.deverytime.domain");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop",
                    "hibernate.physical_naming_strategy",
                    "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl"));
            return factory;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
        @Bean ImageStorageService imageStorageService() {
            return mock(ImageStorageService.class);
        }
        @Bean PostImageCleanupService cleanupService(PostRepository posts, PostImageCleanupRepository pending,
                                                     PostImageRepository images, ImageStorageService storage) {
            return new PostImageCleanupService(posts, pending, images, storage);
        }
        @Bean PostService postService(PostRepository posts, CategoryRepository categories,
                                      PostImageRepository images, PostImageCleanupRepository pending,
                                      ImageStorageService storage, PostImageCleanupService cleanup) {
            return new PostService(posts, mock(UserRepository.class), categories, images,
                    mock(PostViewLogRepository.class), mock(PostLikeRepository.class),
                    storage, mock(CommentRepository.class), pending, cleanup);
        }
    }
}
