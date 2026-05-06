package com.cc91.repository;

import com.cc91.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 通知数据访问层
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 按用户ID查询通知列表（按创建时间倒序）
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 按用户ID分页查询通知列表（按创建时间倒序）
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 统计用户未读通知数量
     */
    Long countByUserIdAndIsReadFalse(Long userId);
}
