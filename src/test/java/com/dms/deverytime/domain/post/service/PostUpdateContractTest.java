package com.dms.deverytime.domain.post.service;

import com.dms.deverytime.domain.category.entity.Category;
import com.dms.deverytime.domain.category.repository.CategoryRepository;
import com.dms.deverytime.domain.comment.repository.CommentRepository;
import com.dms.deverytime.domain.post.dto.request.PostUpdateRequest;
import com.dms.deverytime.domain.post.entity.*;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(PostUpdateContractTest.Config.class)
class PostUpdateContractTest {
    @Autowired PostService service;
    @Autowired PostRepository posts;
    @Autowired EntityManagerFactory factory;
    @Autowired PlatformTransactionManager manager;
    private static final AtomicInteger sequence = new AtomicInteger(3000);
    private TransactionTemplate tx;
    private EntityManager em;
    private Long postId, userId, originalCategoryId, newCategoryId;
    private String keyword;

    @BeforeEach
    void setup() {
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
        var request = request(null, PostCreateStatus.PUBLISHED);
        ReflectionTestUtils.setField(request, "title", keyword);
        service.updatePost(postId, request, userId);
        assertThat(posts.findByCategoryIdWithCategory(originalCategoryId, pageable)).extracting(Post::getId)
                .containsExactly(postId);
        assertThat(posts.findByTitleContainingWithCategory(keyword, pageable)).extracting(Post::getId)
                .containsExactly(postId);
        assertThat(posts.findAllByUser_Id(userId, pageable)).extracting(Post::getId).containsExactly(postId);
    }

    private PostUpdateRequest request(Long categoryId, PostCreateStatus status) {
        var request = new PostUpdateRequest();
        ReflectionTestUtils.setField(request, "title", "updated");
        ReflectionTestUtils.setField(request, "content", "updated content");
        ReflectionTestUtils.setField(request, "categoryId", categoryId);
        ReflectionTestUtils.setField(request, "status", status);
        return request;
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
        @Bean PostService postService(PostRepository posts, CategoryRepository categories,
                                      PostImageRepository images) {
            return new PostService(posts, mock(UserRepository.class), categories, images,
                    mock(PostViewLogRepository.class), mock(PostLikeRepository.class),
                    mock(ImageStorageService.class), mock(CommentRepository.class));
        }
    }
}
