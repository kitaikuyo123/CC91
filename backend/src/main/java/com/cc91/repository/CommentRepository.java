package com.cc91.repository;

import com.cc91.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 评论仓储接口
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 根据帖子ID查询所有评论（包括已删除的）
     */
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    /**
     * 根据帖子ID和状态查询评论
     */
    List<Comment> findByPostIdAndStatusOrderByCreatedAtAsc(Long postId, String status);

    /**
     * 根据父评论ID查询回复
     */
    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);
}
