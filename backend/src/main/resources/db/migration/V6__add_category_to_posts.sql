-- 为帖子表添加版块分类外键
ALTER TABLE posts
ADD COLUMN category_id BIGINT NULL COMMENT '版块ID' AFTER author_id,
ADD CONSTRAINT fk_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
ADD INDEX idx_category_id (category_id);
