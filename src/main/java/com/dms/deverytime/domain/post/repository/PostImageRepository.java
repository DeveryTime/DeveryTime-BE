package com.dms.deverytime.domain.post.repository;

import com.dms.deverytime.domain.post.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {
    List<PostImage> findByPostIdOrderBySortOrder(Long postId);
    Optional<PostImage> findByIdAndPostId(Long id, Long postId);
    void deleteByPostId(Long postId);
}
