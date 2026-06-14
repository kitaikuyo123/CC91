package com.cc91.notificationservice.service;

import com.cc91.notificationservice.dto.NotificationDTO;
import com.cc91.notificationservice.entity.Notification;
import com.cc91.notificationservice.exception.ResourceNotFoundException;
import com.cc91.notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知服务
 */
@Service
public class NotificationServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * 获取用户的通知列表
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return notifications.stream()
                .map(this::toNotificationDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户未读通知数量
     */
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * 标记通知为已读
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("通知不存在"));

        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限操作此通知");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);

        logger.info("通知已标记为已读: id={}, userId={}", notificationId, userId);
    }

    /**
     * 标记所有通知为已读
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);

        logger.info("用户所有通知已标记为已读: userId={}", userId);
    }

    /**
     * 创建通知
     */
    @Transactional
    public void createNotification(Long userId, String type, String title,
                                   String content, Long relatedId) {
        Notification notification = new Notification(userId, type, title, content, relatedId);
        notificationRepository.save(notification);

        logger.info("通知创建成功: userId={}, type={}, relatedId={}", userId, type, relatedId);
    }

    /**
     * 删除通知
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("通知不存在"));

        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限删除此通知");
        }

        notificationRepository.delete(notification);

        logger.info("通知已删除: id={}, userId={}", notificationId, userId);
    }

    /**
     * 转换为 NotificationDTO
     */
    private NotificationDTO toNotificationDTO(Notification notification) {
        return new NotificationDTO(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getRelatedId(),
                notification.getIsRead(),
                notification.getCreatedAt()
        );
    }
}
