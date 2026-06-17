-- 复合索引：优化 500 并发下的高频查询路径
-- 来源：500 并发压测暴露的 scenario-1/2a/6 慢查询根因
--       单列索引（idx_posts_status / idx_posts_category_id / idx_comments_post_id 等）
--       无法被组合成范围扫描，导致回表 + filesort。

-- posts 表：按分类 + 状态分页查询（PostService.getPostsByCategory）
-- 查询模式：WHERE category_id = ? AND status = ? ORDER BY created_at DESC
CREATE INDEX idx_posts_category_status_created
    ON posts (category_id, status, created_at DESC);

-- posts 表：按状态分页查询（PostService.getPostList 默认 latest 排序）
-- 查询模式：WHERE status = ? ORDER BY created_at DESC
-- 覆盖 category_id IS NULL 的全表 fallback 路径。
CREATE INDEX idx_posts_status_created
    ON posts (status, created_at DESC);

-- comments 表：按帖子 + 状态查询评论列表（CommentService.getCommentsByPostId）
-- 查询模式：WHERE post_id = ? AND status = ? ORDER BY created_at ASC
-- 注意排序方向是 ASC（评论时间正序）。
CREATE INDEX idx_comments_post_status_created
    ON comments (post_id, status, created_at ASC);
