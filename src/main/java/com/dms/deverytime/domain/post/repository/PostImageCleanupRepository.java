package com.dms.deverytime.domain.post.repository;

import com.dms.deverytime.domain.post.entity.PostImageCleanup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostImageCleanupRepository extends JpaRepository<PostImageCleanup, Long> {
    @Query("SELECT c FROM PostImageCleanup c WHERE c.postId = :postId AND c.completed = false ORDER BY c.id")
    List<PostImageCleanup> findByPostIdOrderById(@Param("postId") Long postId);

    List<PostImageCleanup> findByPostIdAndRetiredTrue(Long postId);
}
