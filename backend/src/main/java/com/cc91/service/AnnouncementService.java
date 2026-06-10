package com.cc91.service;

import com.cc91.dto.AnnouncementDTO;
import com.cc91.dto.CreateAnnouncementRequest;
import com.cc91.dto.UpdateAnnouncementRequest;
import com.cc91.entity.Announcement;
import com.cc91.entity.User;
import com.cc91.exception.ResourceNotFoundException;
import com.cc91.repository.AnnouncementRepository;
import com.cc91.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 公告服务
 */
@Service
public class AnnouncementService {

    private static final Logger logger = LoggerFactory.getLogger(AnnouncementService.class);

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AnnouncementService(AnnouncementRepository announcementRepository,
                               UserRepository userRepository,
                               NotificationService notificationService) {
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * 获取所有公告（置顶优先，时间倒序）
     */
    @Transactional(readOnly = true)
    public List<AnnouncementDTO> findAll() {
        return announcementRepository.findAllWithAuthor()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取公告
     */
    @Transactional(readOnly = true)
    public AnnouncementDTO findById(Long id) {
        Announcement announcement = announcementRepository.findByIdWithAuthor(id)
                .orElseThrow(() -> new ResourceNotFoundException("公告不存在"));
        return toDTO(announcement);
    }

    /**
     * 创建公告
     */
    @Transactional
    public AnnouncementDTO create(CreateAnnouncementRequest request, String username) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        Announcement announcement = new Announcement(
                request.getTitle(),
                request.getContent(),
                author.getId()
        );
        announcement.setAuthor(author);  // 设置关联以支持 toDTO 直接读取 username，避免 lazy-loading 失败
        if (request.getIsPinned() != null) {
            announcement.setIsPinned(request.getIsPinned());
        }

        announcement = announcementRepository.save(announcement);

        // 通知所有用户新公告（try-catch 防止通知失败回滚公告保存）
        try {
            List<User> allUsers = userRepository.findAll();
            for (User user : allUsers) {
                notificationService.createNotification(
                        user.getId(), "SYSTEM", "新公告: " + announcement.getTitle(),
                        announcement.getContent(), announcement.getId()
                );
            }
            logger.info("公告创建成功: id={}, title={}, 已通知{}位用户",
                    announcement.getId(), announcement.getTitle(), allUsers.size());
        } catch (Exception e) {
            logger.warn("公告通知发送失败，但公告已保存: id={}, error={}",
                    announcement.getId(), e.getMessage());
        }

        return toDTO(announcement);
    }

    /**
     * 更新公告
     */
    @Transactional
    public AnnouncementDTO update(Long id, UpdateAnnouncementRequest request) {
        Announcement announcement = announcementRepository.findByIdWithAuthor(id)
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

        return toDTO(announcement);
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
     * 转换为 AnnouncementDTO
     */
    private AnnouncementDTO toDTO(Announcement announcement) {
        String authorUsername = announcement.getAuthor() != null
                ? announcement.getAuthor().getUsername()
                : "未知用户";

        return new AnnouncementDTO(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getAuthorId(),
                authorUsername,
                announcement.getIsPinned(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }
}
