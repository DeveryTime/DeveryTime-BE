package com.dms.deverytime.domain.post.repository;

import com.dms.deverytime.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 카테고리아이디로 게시글 분배 역할
    Page<Post> findByCategoryId(Long categoryId, Pageable pageable);

    // 게시글 제목 검색
    Page<Post> findByTitleContaining(String keyword, Pageable pageable);

    // 사용자 검색 - username
    Page<Post> findByUserUsernameContaining(String userKeyword, Pageable pageable);

    //게시글이랑 카테고리를 한번에 가져옴 (N+1 문제 해결)
    @Query("SELECT p FROM Post p JOIN FETCH p.category")
    Page<Post> findAllWithCategory(Pageable pageable);

}
