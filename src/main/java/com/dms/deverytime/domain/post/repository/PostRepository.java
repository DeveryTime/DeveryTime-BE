package com.dms.deverytime.domain.post.repository;

import com.dms.deverytime.domain.post.entity.Post;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 이미지 수 확인부터 저장까지 동일 게시글의 업로드 요청을 직렬화한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Post p WHERE p.id = :id")
    Optional<Post> findByIdForImageUpload(@Param("id") Long id);

    //게시글이랑 카테고리를 한번에 가져옴 (N+1 문제 해결)
    @Query("SELECT p FROM Post p JOIN FETCH p.category WHERE p.status = com.dms.deverytime.domain.post.enums.PostCreateStatus.PUBLISHED")
    Page<Post> findAllWithCategory(Pageable pageable);


    @Query("SELECT p FROM Post p JOIN FETCH p.category WHERE p.category.id = :categoryId AND p.status = com.dms.deverytime.domain.post.enums.PostCreateStatus.PUBLISHED")
    Page<Post> findByCategoryIdWithCategory(@Param("categoryId") Long categoryId, Pageable pageable);

    // 특정 사용자가 작성한 게시글 조회
    @EntityGraph(attributePaths = "category")
    Page<Post> findAllByUser_Id(Long userId, Pageable pageable);

    // LIKE %:keyword%로 keyword가 title 안에 포함돼 있으면 매치
    // JOIN FETCH p.user: getUser().getProfileImageUrl() 호출 시 게시글마다 User 쿼리가 나가는 N+1 방지
    @Query("SELECT p FROM Post p JOIN FETCH p.category JOIN FETCH p.user WHERE p.title LIKE %:keyword% AND p.status = com.dms.deverytime.domain.post.enums.PostCreateStatus.PUBLISHED")
    Page<Post> findByTitleContainingWithCategory(@Param("keyword") String keyword, Pageable pageable);

}
