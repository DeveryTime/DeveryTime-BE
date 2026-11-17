-- 파일 정리 성공 후에도 이전 URL 이력을 보관하여 오래된 본문 저장을 막는다.
ALTER TABLE post_image_cleanup
    ADD COLUMN retired BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN completed BOOLEAN NOT NULL DEFAULT FALSE;
