package com.cc91.forumservice.repository;

import com.cc91.forumservice.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子数据访问层
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 按作者ID查询帖子列表
     */
    List<Post> findByAuthorId(Long authorId);

    /**
     * 按作者ID查询帖子列表（按时间倒序）
     */
    List<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    /**
     * 按作者ID和状态查询帖子列表（按时间倒序）
     */
    List<Post> findByAuthorIdAndStatusOrderByCreatedAtDesc(Long authorId, String status);

    /**
     * 按作者ID分页查询帖子
     */
    Page<Post> findByAuthorId(Long authorId, Pageable pageable);

    /**
     * 按状态分页查询帖子
     */
    Page<Post> findByStatus(String status, Pageable pageable);

    /**
     * 按版块ID和状态分页查询帖子
     */
    Page<Post> findByCategoryIdAndStatus(Long categoryId, String status, Pageable pageable);

    /**
     * 统计某分类下的帖子数量（所有状态，用于删除前检查）
     */
    long countByCategoryId(Long categoryId);

    /**
     * 统计某分类下的已发布帖子数量
     */
    long countByCategoryIdAndStatus(Long categoryId, String status);

    long countByCategoryIdAndStatusAndCreatedAtAfter(Long categoryId, String status, LocalDateTime since);

    /**
     * 批量统计各版块的已发布帖子数和今日新帖数（单次查询）
     */
    @Query("SELECT p.categoryId, COUNT(p), " +
           "SUM(CASE WHEN p.createdAt >= :todayStart THEN 1 ELSE 0 END) " +
           "FROM Post p WHERE p.status = :status GROUP BY p.categoryId")
    List<Object[]> countStatsByCategory(@Param("status") String status, @Param("todayStart") LocalDateTime todayStart);

    /**
     * 按标题或内容搜索帖子（分页）
     * 只返回指定状态的帖子
     */
    Page<Post> findByStatusAndTitleContainingOrStatusAndContentContaining(
            String status, String titleKeyword,
            String status2, String contentKeyword,
            Pageable pageable
    );

    /**
     * 原子更新浏览量（避免并发竞态条件）
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
