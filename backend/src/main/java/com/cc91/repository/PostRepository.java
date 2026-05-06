package com.cc91.repository;

import com.cc91.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
     * 按标题或内容搜索帖子（分页）
     * 只返回指定状态的帖子
     */
    Page<Post> findByStatusAndTitleContainingOrStatusAndContentContaining(
            String status, String titleKeyword,
            String status2, String contentKeyword,
            Pageable pageable
    );
}
