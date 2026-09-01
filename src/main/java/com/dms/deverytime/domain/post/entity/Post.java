package com.dms.deverytime.domain.post.entity;

import com.dms.deverytime.domain.category.entity.Category;
import com.dms.deverytime.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Table(name = "post")

// 파라미터 X 기본 생성자를 자동 생성
@NoArgsConstructor
// 이 클래스가 DB 테이블과 매핑되는 객체임을 선언
@Entity
//모든 필드의 getter 메서드를 롬복이 컴파일 시점에 자동으로 생성
@Getter
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //User 정보가 필요할때만 DB에서 가져오는 LAZY, 작성자없는 글은 조회 X -> nullable = false
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    //카테고리 필수
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    //제목 필수, 최대 255자
    @Column(nullable = false, length = 255)
    private String title;

    //내용 필수, columnDefinition = "TEXT"를 써서 길이 제한 두지 않고 긴 글 저장 가능
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    //상태 필수, "DRAFT" or "PUBLISHED" 때문에 10글자 이내
    @Column(nullable = false, length = 10)
    private String status;

    //int라 null X
    @Column(nullable = false)
    private int viewCount;

    //생성일자 필수, 업데이트해도 바뀌지 않게
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    //수정시각은 @Column X -> null 허용
    private LocalDateTime updatedAt;

    // @PrePersist = 객체가 DB에 저장되기 전에 JPA로 인해 자동으로 호출되는 메서드, 생성시간을 게시글 저장하기 직전 지금 시간으로 지정하겠다.
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // @PreUpdate = 업데이트 하기 전에 JPA로 인해 자동으로 호출되는 메서드, 수정시간을 업데이트 하기 직전 지금 시간으로 지정하겠다.
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Post(User user, Category category, String title, String content, String status) {
        this.user = user;
        this.category = category;
        this.title = title;
        this.content = content;
        this.status = status;
        this.viewCount = 0;
    }

    //수정은 제목, 내용만. 카테고리 수정 여부는 나중에
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    //조회수 1씩 증가
    public void increaseViewCount() {
        this.viewCount += 1;
    }
}
