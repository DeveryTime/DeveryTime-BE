package com.dms.deverytime.domain.post.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import com.dms.deverytime.domain.user.entity.User;

@Entity

 // 중복 방지용으로 DB 차원에서 똑같은 게시글을 똑같은 유저가 두번 이상 조회 못하도록 DB에 들어가는 컬럼을 통제
@Table(
        name = "post_view_log",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
@Getter
@NoArgsConstructor
public class PostViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //여러개의 조회기록이 하나의 게시글을 가르키는 관, 포스트_아이디라는 컬럼으로 저장되며 그게 없으면 안됨
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    //한 유저가 여러개의 조회기록을 남기는 관
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public PostViewLog(Post post, User user) {
        this.post = post;
        this.user = user;
    }
}
