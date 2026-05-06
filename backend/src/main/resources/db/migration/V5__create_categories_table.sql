-- 版块分类表
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '版块ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '版块名称',
    description VARCHAR(500) COMMENT '版块描述',
    sort_order INT DEFAULT 0 COMMENT '排序顺序（数字越小越靠前）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='版块分类表';

-- 插入初始版块数据
INSERT INTO categories (name, description, sort_order) VALUES
('技术讨论', '分享编程技术、开发经验和问题解决方案', 1),
('灌水区', '日常闲聊、非技术话题交流', 2),
('资源分享', '分享学习资源、工具和项目', 3),
('招聘求职', '技术岗位招聘与求职信息', 4);
