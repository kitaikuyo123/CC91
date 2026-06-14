-- ============================================================
-- CC91 压力测试种子数据
-- 生成: 10 个测试用户 + 1 万篇帖子 + 3 万条评论
-- 用法: mysql -u root -p cc91_db < seed_data.sql
-- ============================================================

-- ===== 1. 创建 10 个测试用户（密码均为 test123，BCrypt 哈希）=====
INSERT IGNORE INTO users (username, email, password_hash, role) VALUES
('testuser1',  'test1@test.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER'),
('testuser2',  'test2@test.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER'),
('testuser3',  'test3@test.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER'),
('testuser4',  'test4@test.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER'),
('testuser5',  'test5@test.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER'),
('testuser6',  'test6@test.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER'),
('testuser7',  'test7@test.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER'),
('testuser8',  'test8@test.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER'),
('testuser9',  'test9@test.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER'),
('testuser10', 'test10@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER');

-- 为测试用户创建 profile
INSERT IGNORE INTO user_profiles (user_id, avatar_url, bio, created_at) 
SELECT id, NULL, CONCAT('我是 ', username), NOW() FROM users WHERE username LIKE 'testuser%';

-- ===== 2. 批量插入 1 万篇帖子 =====
DROP PROCEDURE IF EXISTS seed_posts;
DELIMITER //
CREATE PROCEDURE seed_posts(IN num INT)
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE uid BIGINT;
  DECLARE cid INT;
  DECLARE rand_days INT;
  
  WHILE i <= num DO
    SET uid = FLOOR(1 + RAND() * 10);
    SET cid = FLOOR(1 + RAND() * 4);
    SET rand_days = FLOOR(RAND() * 365);
    
    INSERT INTO posts (title, content, author_id, category_id, view_count, like_count, status, created_at, updated_at)
    VALUES (
      CONCAT('测试帖子 #', i, ' - ', 
        CASE cid WHEN 1 THEN 'Java Spring Boot 微服务实践分享'
                 WHEN 2 THEN '大家今天吃了什么好吃的？'
                 WHEN 3 THEN '推荐几个好用的 VS Code 插件'
                 WHEN 4 THEN '招聘前端开发工程师（远程）' END),
      CONCAT('这是第 ', i, ' 篇测试帖子的正文内容。\n\n',
             '本帖子属于压力测试种子数据，用于模拟万级数据量下的系统性能表现。\n\n',
             '讨论内容涵盖：技术交流、日常闲聊、资源推荐等校园论坛常见话题。\n\n',
             REPEAT('填充段落以模拟真实帖子长度。', 10)),
      uid,
      cid,
      FLOOR(RAND() * 5000),
      FLOOR(RAND() * 200),
      'PUBLISHED',
      DATE_SUB(NOW(), INTERVAL rand_days DAY),
      DATE_SUB(NOW(), INTERVAL rand_days DAY)
    );
    SET i = i + 1;
  END WHILE;
END //
DELIMITER ;

CALL seed_posts(10000);
DROP PROCEDURE seed_posts;

-- ===== 3. 批量插入 3 万条评论 =====
DROP PROCEDURE IF EXISTS seed_comments;
DELIMITER //
CREATE PROCEDURE seed_comments(IN num INT)
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE pid BIGINT;
  DECLARE uid BIGINT;
  DECLARE rand_days INT;
  
  WHILE i <= num DO
    SET pid = FLOOR(1 + RAND() * 10000);
    SET uid = FLOOR(1 + RAND() * 10);
    SET rand_days = FLOOR(RAND() * 365);
    
    INSERT INTO comments (post_id, author_id, content, parent_id, status, created_at, updated_at)
    VALUES (
      pid,
      uid,
      CASE FLOOR(RAND() * 5)
        WHEN 0 THEN '感谢分享，学习了！'
        WHEN 1 THEN '这个观点很有意思，支持一下。'
        WHEN 2 THEN '期待后续更新，收藏了。'
        WHEN 3 THEN '有点不同意见，我觉得还需要考虑...'
        WHEN 4 THEN '好帖，顶！' END,
      NULL,
      'PUBLISHED',
      DATE_SUB(NOW(), INTERVAL rand_days DAY),
      DATE_SUB(NOW(), INTERVAL rand_days DAY)
    );
    SET i = i + 1;
  END WHILE;
END //
DELIMITER ;

CALL seed_comments(30000);
DROP PROCEDURE seed_comments;

-- ===== 4. 验证数据量 =====
SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL
SELECT 'posts', COUNT(*) FROM posts
UNION ALL
SELECT 'comments', COUNT(*) FROM comments
UNION ALL
SELECT 'categories', COUNT(*) FROM categories
UNION ALL
SELECT 'notifications', COUNT(*) FROM notifications;
