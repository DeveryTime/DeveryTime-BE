package com.dms.deverytime.domain.post.repository;

import com.dms.deverytime.domain.post.entity.PostImageCleanup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostImageCleanupRepository extends JpaRepository<PostImageCleanup, Long> {
    List<PostImageCleanup> findByPostIdOrderById(Long postId);
}
