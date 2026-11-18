ALTER TABLE post_image_cleanup
    ADD COLUMN upload_candidate BOOLEAN NOT NULL DEFAULT FALSE;
