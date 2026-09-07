package com.dms.deverytime.domain.post.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Table(name = "post_image")
@Getter

public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // 여러개의 사진이 하나의 게시글에 속하는 관계, nullable = false -> 글 없이 사진만 올리기 방
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // URL최대 길이 500
    @Column(nullable = false, length = 500)
    private String imageUrl;

    // 이미지 순서
    @Column(nullable = false)
    private int sortOrder;

    public PostImage(Post post, String imageUrl, int sortOrder) {
        this.post = post;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }

}
