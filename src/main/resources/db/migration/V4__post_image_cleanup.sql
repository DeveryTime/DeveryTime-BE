-- 게시글이 삭제된 후에도 파일 정리 기록을 보존하기 위해 외래키를 두지 않는다.
CREATE TABLE post_image_cleanup (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    public_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_post_image_cleanup_post (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
