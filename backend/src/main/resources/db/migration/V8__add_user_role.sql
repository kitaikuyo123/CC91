-- 为用户表添加角色字段
ALTER TABLE users
ADD COLUMN role VARCHAR(20) DEFAULT 'USER' COMMENT '用户角色: USER, ADMIN' AFTER email,
ADD INDEX idx_role (role);

-- 将 admin 用户设置为 ADMIN 角色
UPDATE users SET role = 'ADMIN' WHERE username = 'admin';
