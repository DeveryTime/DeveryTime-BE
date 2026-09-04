package com.dms.deverytime.domain.post.repository;

import com.dms.deverytime.domain.post.entity.PostViewLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostViewLogRepository extends JpaRepository<PostViewLog, Long> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);
    //누군가 그걸 봤는지 안봤는지 질문
}
