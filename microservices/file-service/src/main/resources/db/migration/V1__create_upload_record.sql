CREATE TABLE IF NOT EXISTS upload_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    uploader_user_id BIGINT NULL COMMENT '上传者 userId，可能为 null（token 解析失败时）',
    uploader_username VARCHAR(64) NOT NULL,
    filename VARCHAR(128) NOT NULL COMMENT '存储的 UUID 文件名（含扩展名）',
    original_filename VARCHAR(255) NULL,
    content_type VARCHAR(64) NOT NULL,
    file_size BIGINT NOT NULL,
    url VARCHAR(255) NOT NULL,
    purpose VARCHAR(32) NOT NULL COMMENT 'IMAGE 或 AVATAR',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_uploader_user_id (uploader_user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
