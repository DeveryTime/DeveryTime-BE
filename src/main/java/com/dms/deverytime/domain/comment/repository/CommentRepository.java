package com.dms.deverytime.domain.comment.repository;

import com.dms.deverytime.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("""
        select c
        from Comment c
        join fetch c.user
        where c.post.id = :postId
        order by c.createdAt asc
        """)
    List<Comment> findAllByPostIdWithUser(@Param("postId") Long postId);
}