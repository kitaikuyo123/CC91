-- ===== 用户 =====
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_locked BIT(1) NOT NULL DEFAULT 0,
    failed_login_attempts INT DEFAULT 0,
    lock_until DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_profiles (
    user_id BIGINT PRIMARY KEY,
    avatar_url VARCHAR(500),
    bio VARCHAR(500),
    location VARCHAR(100),
    website VARCHAR(200),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===== 版块 =====
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500),
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===== 帖子 =====
CREATE TABLE IF NOT EXISTS posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    author_id BIGINT NOT NULL,
    category_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    view_count INT NOT NULL DEFAULT 0,
    like_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    INDEX idx_posts_author_id (author_id),
    INDEX idx_posts_created_at (created_at),
    INDEX idx_posts_status (status),
    INDEX idx_posts_category_id (category_id),
    FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===== 评论 =====
CREATE TABLE IF NOT EXISTS comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    parent_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    INDEX idx_comments_post_id (post_id),
    INDEX idx_comments_author_id (author_id),
    INDEX idx_comments_parent_id (parent_id),
    INDEX idx_comments_created_at (created_at),
    INDEX idx_comments_status (status),
    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (parent_id) REFERENCES comments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===== 点赞 / 收藏 =====
CREATE TABLE IF NOT EXISTS post_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_post_likes_user_post (user_id, post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS post_bookmarks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_post_bookmarks_user_post (user_id, post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===== 通知 =====
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    related_id BIGINT,
    is_read BIT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notifications_user_id (user_id),
    INDEX idx_notifications_user_read (user_id, is_read),
    INDEX idx_notifications_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===== 公告 =====
CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    author_id BIGINT NOT NULL,
    is_pinned BIT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_announcements_created_at (created_at),
    INDEX idx_announcements_is_pinned (is_pinned)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===== 举报 =====
CREATE TABLE IF NOT EXISTS reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(10) NOT NULL,
    target_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_comment VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME,
    INDEX idx_reports_status (status),
    INDEX idx_reports_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ======================================================================
-- 种子数据
-- ======================================================================

-- 管理员（密码: admin123）
INSERT IGNORE INTO users (username, email, password_hash, role) VALUES
('admin', 'admin@cc91.com', '$2b$12$AbBS8jX8QpsvtqePZhFpmOhpCOlPc46rBYbVD0LdnjM8vS.HtihS.', 'ADMIN'),
('user', 'user@cc91.com', '$2b$12$gSfPVajelv7QVxdBkCpz9Os2BH.n/a6x8T7YP9nKKFX0A/BktqOWG', 'USER');

-- 默认版块
INSERT IGNORE INTO categories (name, description, sort_order) VALUES
('技术讨论', '分享编程技术、开发经验和问题解决方案', 1),
('灌水区', '日常闲聊、非技术话题交流', 2),
('资源分享', '分享学习资源、工具和项目', 3),
('招聘求职', '技术岗位招聘与求职信息', 4);

-- 默认公告
INSERT IGNORE INTO announcements (id, title, content, author_id, is_pinned, created_at, updated_at) VALUES
(1, 'CC91 论坛经典 CC98 视觉风格美化重构上线公告', 'CC91 论坛已完成经典 CC98 视觉风格的全面美化重构，新版界面在保留经典 BBS 布局的基础上，引入了响应式设计与现代化交互体验。主要更新包括：\n\n1. 全新 CC98 复古金色主题配色方案\n2. 双栏布局：左栏版块与帖子列表，右栏热门推荐\n3. 公告栏经典粗顶部边框样式\n4. 移动端自适应优化\n\n感谢各位用户的支持，欢迎体验并提出反馈！', 1, 1, '2026-05-28 23:07:08', '2026-05-28 23:07:08'),
(2, '关于规范社区讨论、禁止灌水与文明发言的通知', '为营造良好的社区氛围，CC91 论坛现就发言规范作如下通知：\n\n1. 禁止在非灌水版块发布无意义内容（如纯表情、单字回复等）\n2. 禁止人身攻击、侮辱性语言及任何形式的歧视言论\n3. 讨论应围绕主题展开，不得恶意歪楼\n4. 违规者将视情节给予警告或禁言处理\n\n请各位用户自觉遵守，共同维护社区环境。', 1, 0, '2026-05-28 23:07:08', '2026-05-28 23:07:08'),
(3, '推荐使用主流现代浏览器以获得最佳体验', 'CC91 论坛采用现代化前端技术构建，为确保最佳浏览体验，推荐使用以下浏览器：\n\n• Google Chrome（推荐版本 100+）\n• Microsoft Edge（推荐版本 100+）\n• Safari（推荐版本 15+）\n• Firefox（推荐版本 100+）\n\n不建议使用 IE 浏览器访问，部分功能可能无法正常使用。', 1, 0, '2026-05-28 23:07:08', '2026-05-28 23:07:08');
