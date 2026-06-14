package com.cc91.contentservice.service;

import com.cc91.contentservice.client.CreateNotificationRequest;
import com.cc91.contentservice.client.NotificationServiceClient;
import com.cc91.contentservice.client.UserInfoDTO;
import com.cc91.contentservice.client.UserServiceClient;
import com.cc91.contentservice.dto.AnnouncementDTO;
import com.cc91.contentservice.dto.CreateAnnouncementRequest;
import com.cc91.contentservice.dto.UpdateAnnouncementRequest;
import com.cc91.contentservice.entity.Announcement;
import com.cc91.contentservice.exception.ResourceNotFoundException;
import com.cc91.contentservice.repository.AnnouncementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 公告服务
 * Microservice version: replaces UserRepository with UserServiceClient Feign,
 * NotificationService with NotificationServiceClient Feign.
 */
@Service
public class AnnouncementService {

    private static final Logger logger = LoggerFactory.getLogger(AnnouncementService.class);

    private final AnnouncementRepository announcementRepository;
    private final UserServiceClient userServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    public AnnouncementService(AnnouncementRepository announcementRepository,
                               UserServiceClient userServiceClient,
                               NotificationServiceClient notificationServiceClient) {
        this.announcementRepository = announcementRepository;
        this.userServiceClient = userServiceClient;
        this.notificationServiceClient = notificationServiceClient;
    }

    /**
     * 获取所有公告（置顶优先，时间倒序）
     */
    @Transactional(readOnly = true)
    public List<AnnouncementDTO> findAll() {
        List<Announcement> announcements = announcementRepository.findAll(
                Sort.by(Sort.Direction.DESC, "isPinned")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        if (announcements.isEmpty()) {
            return List.of();
        }

        // Batch fetch author info via Feign
        List<Long> authorIds = announcements.stream()
                .map(Announcement::getAuthorId)
                .distinct()
                .toList();

        Map<Long, UserInfoDTO> authorMap = fetchAuthorMap(authorIds);

        return announcements.stream()
                .map(a -> toDTO(a, authorMap.getOrDefault(a.getAuthorId(),
                        new UserInfoDTO(a.getAuthorId(), "未知用户", "USER", null))))
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取公告
     */
    @Transactional(readOnly = true)
    public AnnouncementDTO findById(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("公告不存在"));

        // Single fetch author info via Feign
        String authorUsername = "未知用户";
        try {
            UserInfoDTO author = userServiceClient.getUserById(announcement.getAuthorId());
            if (author != null && author.getUsername() != null) {
                authorUsername = author.getUsername();
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch author for announcement {}: {}", id, e.getMessage());
        }

        return toDTO(announcement, new UserInfoDTO(announcement.getAuthorId(), authorUsername, "USER", null));
    }

    /**
     * 创建公告
     */
    @Transactional
    public AnnouncementDTO create(CreateAnnouncementRequest request, String username) {
        // Resolve author via Feign
        UserInfoDTO author = userServiceClient.getUserByUsername(username);
        if (author == null || author.getId() == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        Announcement announcement = new Announcement(
                request.getTitle(),
                request.getContent(),
                author.getId()
        );
        if (request.getIsPinned() != null) {
            announcement.setIsPinned(request.getIsPinned());
        }

        announcement = announcementRepository.save(announcement);

        // Notify all users about the new announcement (best-effort, non-blocking)
        try {
            // Get all user IDs - we use a large batch to get all users
            // In a real system this would be a paginated or event-driven approach
            List<UserInfoDTO> allUsers = userServiceClient.getUsersByIds(List.of());
            for (UserInfoDTO user : allUsers) {
                try {
                    notificationServiceClient.createNotification(new CreateNotificationRequest(
                            user.getId(), "ANNOUNCEMENT", "新公告: " + announcement.getTitle(),
                            announcement.getContent(), announcement.getId()
                    ));
                } catch (Exception ex) {
                    logger.debug("Failed to notify userId={}, continuing: {}", user.getId(), ex.getMessage());
                }
            }
            logger.info("公告创建成功: id={}, title={}, 已通知{}位用户",
                    announcement.getId(), announcement.getTitle(), allUsers.size());
        } catch (Exception e) {
            logger.warn("公告通知发送失败，但公告已保存: id={}, error={}",
                    announcement.getId(), e.getMessage());
        }

        return toDTO(announcement, author);
    }

    /**
     * 更新公告
     */
    @Transactional
    public AnnouncementDTO update(Long id, UpdateAnnouncementRequest request) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("公告不存在"));

        if (request.getTitle() != null) {
            announcement.setTitle(request.getTitle());
        }

        if (request.getContent() != null) {
            announcement.setContent(request.getContent());
        }

        if (request.getIsPinned() != null) {
            announcement.setIsPinned(request.getIsPinned());
        }

        announcement = announcementRepository.save(announcement);

        logger.info("公告更新成功: id={}, title={}", announcement.getId(), announcement.getTitle());

        // Fetch author info for response
        String authorUsername = "未知用户";
        try {
            UserInfoDTO author = userServiceClient.getUserById(announcement.getAuthorId());
            if (author != null && author.getUsername() != null) {
                authorUsername = author.getUsername();
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch author for announcement {}: {}", id, e.getMessage());
        }

        return toDTO(announcement, new UserInfoDTO(announcement.getAuthorId(), authorUsername, "USER", null));
    }

    /**
     * 删除公告
     */
    @Transactional
    public void delete(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("公告不存在"));

        announcementRepository.delete(announcement);

        logger.info("公告删除成功: id={}, title={}", id, announcement.getTitle());
    }

    /**
     * Batch fetch author info and return as a map keyed by user ID.
     * Returns empty map on failure.
     */
    private Map<Long, UserInfoDTO> fetchAuthorMap(List<Long> authorIds) {
        try {
            List<UserInfoDTO> authors = userServiceClient.getUsersByIds(authorIds);
            return authors.stream()
                    .collect(Collectors.toMap(UserInfoDTO::getId, Function.identity()));
        } catch (Exception e) {
            logger.warn("Failed to batch fetch authors, using fallback: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * Convert Announcement entity + author info to DTO
     */
    private AnnouncementDTO toDTO(Announcement announcement, UserInfoDTO author) {
        return new AnnouncementDTO(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getAuthorId(),
                author != null ? author.getUsername() : "未知用户",
                announcement.getIsPinned(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }
}
