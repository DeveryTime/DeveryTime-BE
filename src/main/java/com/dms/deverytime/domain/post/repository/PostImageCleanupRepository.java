package com.dms.deverytime.domain.post.repository;

import com.dms.deverytime.domain.post.entity.PostImageCleanup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface PostImageCleanupRepository extends JpaRepository<PostImageCleanup, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM PostImageCleanup c WHERE c.id = :id")
    Optional<PostImageCleanup> findByIdForUpdate(@Param("id") Long id);
    @Query("SELECT c FROM PostImageCleanup c WHERE c.postId = :postId AND c.completed = false ORDER BY c.id")
    List<PostImageCleanup> findByPostIdOrderById(@Param("postId") Long postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM PostImageCleanup c WHERE c.postId = :postId AND c.completed = false ORDER BY c.id")
    List<PostImageCleanup> findPendingForUpdate(@Param("postId") Long postId);

    List<PostImageCleanup> findByPostIdAndRetiredTrue(Long postId);

    @Query("SELECT DISTINCT c.postId FROM PostImageCleanup c WHERE c.completed = false AND " +
            "(c.retired = true OR c.uploadCandidate = true OR NOT EXISTS (SELECT p.id FROM Post p WHERE p.id = c.postId))")
    List<Long> findPostIdsWithRetryableCleanup();
}
