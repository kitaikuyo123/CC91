package com.cc91.service;

import com.cc91.dto.AnnouncementDTO;
import com.cc91.dto.CreateAnnouncementRequest;
import com.cc91.dto.UpdateAnnouncementRequest;
import com.cc91.entity.Announcement;
import com.cc91.entity.User;
import com.cc91.exception.ResourceNotFoundException;
import com.cc91.repository.AnnouncementRepository;
import com.cc91.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AnnouncementService business logic tests
 */
@SpringBootTest
@ActiveProfiles("test")
class AnnouncementServiceTest {

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    @Transactional
    void cleanDatabase() {
        announcementRepository.deleteAll();
        announcementRepository.flush();
        userRepository.deleteAll();
        userRepository.flush();
    }

    private User createTestUser() {
        User user = new User("test_user", "test@example.com", passwordEncoder.encode("password123"));
        return userRepository.save(user);
    }

    // ==================== findAll ====================

    @Test
    @Transactional
    void findAll_ReturnsSortedByPinnedThenCreatedAtDesc() {
        // Arrange
        User user = createTestUser();
        Announcement a1 = new Announcement("普通公告1", "内容1", user.getId());
        a1.setIsPinned(false);
        announcementRepository.saveAndFlush(a1);

        Announcement a2 = new Announcement("置顶公告", "内容2", user.getId());
        a2.setIsPinned(true);
        announcementRepository.saveAndFlush(a2);

        Announcement a3 = new Announcement("普通公告2", "内容3", user.getId());
        a3.setIsPinned(false);
        announcementRepository.saveAndFlush(a3);

        // Act
        List<AnnouncementDTO> announcements = announcementService.findAll();

        // Assert: 置顶优先，同组内时间倒序
        assertNotNull(announcements);
        assertEquals(3, announcements.size());
        assertTrue(announcements.get(0).getIsPinned());
        assertEquals("置顶公告", announcements.get(0).getTitle());
        assertFalse(announcements.get(1).getIsPinned());
        assertFalse(announcements.get(2).getIsPinned());
        assertEquals("普通公告2", announcements.get(1).getTitle());
        assertEquals("普通公告1", announcements.get(2).getTitle());
    }

    @Test
    void findAll_EmptyDatabase_ReturnsEmptyList() {
        List<AnnouncementDTO> announcements = announcementService.findAll();
        assertNotNull(announcements);
        assertTrue(announcements.isEmpty());
    }

    // ==================== findById ====================

    @Test
    @Transactional
    void findById_ExistingAnnouncement_ReturnsAnnouncementDTO() {
        // Arrange
        User user = createTestUser();
        Announcement announcement = new Announcement("测试公告", "测试内容", user.getId());
        announcement.setAuthor(user);  // 显式设置关联，避免一级缓存返回 author=null
        announcement.setIsPinned(true);
        announcement = announcementRepository.saveAndFlush(announcement);

        // Act
        AnnouncementDTO dto = announcementService.findById(announcement.getId());

        // Assert
        assertNotNull(dto);
        assertEquals(announcement.getId(), dto.getId());
        assertEquals("测试公告", dto.getTitle());
        assertEquals("测试内容", dto.getContent());
        assertTrue(dto.getIsPinned());
        assertEquals(user.getId(), dto.getAuthorId());
        assertEquals(user.getUsername(), dto.getAuthorUsername());
        assertNotNull(dto.getCreatedAt());
    }

    @Test
    void findById_NonExistingAnnouncement_ThrowsResourceNotFoundException() {
        Exception exception = assertThrows(ResourceNotFoundException.class,
                () -> announcementService.findById(999L));
        assertEquals("公告不存在", exception.getMessage());
    }

    // ==================== create ====================

    @Test
    @Transactional
    void create_ValidRequest_ReturnsAnnouncementDTO() {
        // Arrange
        User user = createTestUser();
        CreateAnnouncementRequest request = new CreateAnnouncementRequest();
        request.setTitle("新公告");
        request.setContent("新内容");

        // Act
        AnnouncementDTO dto = announcementService.create(request, user.getUsername());

        // Assert
        assertNotNull(dto);
        assertNotNull(dto.getId());
        assertEquals("新公告", dto.getTitle());
        assertEquals("新内容", dto.getContent());
        assertEquals(user.getId(), dto.getAuthorId());
        assertEquals(user.getUsername(), dto.getAuthorUsername());
        assertFalse(dto.getIsPinned());
        assertNotNull(dto.getCreatedAt());
    }

    @Test
    @Transactional
    void create_WithIsPinnedTrue_ReturnsPinnedAnnouncement() {
        // Arrange
        User user = createTestUser();
        CreateAnnouncementRequest request = new CreateAnnouncementRequest();
        request.setTitle("置顶公告");
        request.setContent("重要内容");
        request.setIsPinned(true);

        // Act
        AnnouncementDTO dto = announcementService.create(request, user.getUsername());

        // Assert
        assertNotNull(dto);
        assertTrue(dto.getIsPinned());
        assertEquals("置顶公告", dto.getTitle());
    }

    @Test
    void create_NonExistingUser_ThrowsResourceNotFoundException() {
        CreateAnnouncementRequest request = new CreateAnnouncementRequest();
        request.setTitle("标题");
        request.setContent("内容");

        Exception exception = assertThrows(ResourceNotFoundException.class,
                () -> announcementService.create(request, "nonexistent_user"));
        assertEquals("用户不存在", exception.getMessage());
    }

    // ==================== update ====================

    @Test
    @Transactional
    void update_AllFields_ReturnsUpdatedAnnouncementDTO() {
        // Arrange
        User user = createTestUser();
        Announcement announcement = new Announcement("旧标题", "旧内容", user.getId());
        announcement.setIsPinned(false);
        Long id = announcementRepository.saveAndFlush(announcement).getId();

        UpdateAnnouncementRequest request = new UpdateAnnouncementRequest();
        request.setTitle("新标题");
        request.setContent("新内容");
        request.setIsPinned(true);

        // Act
        AnnouncementDTO dto = announcementService.update(id, request);

        // Assert
        assertNotNull(dto);
        assertEquals(id, dto.getId());
        assertEquals("新标题", dto.getTitle());
        assertEquals("新内容", dto.getContent());
        assertTrue(dto.getIsPinned());
    }

    @Test
    @Transactional
    void update_OnlyTitle_UpdatesOnlyTitle() {
        // Arrange
        User user = createTestUser();
        Announcement announcement = new Announcement("旧标题", "旧内容", user.getId());
        announcement.setIsPinned(false);
        Long id = announcementRepository.saveAndFlush(announcement).getId();

        UpdateAnnouncementRequest request = new UpdateAnnouncementRequest();
        request.setTitle("新标题");

        // Act
        AnnouncementDTO dto = announcementService.update(id, request);

        // Assert
        assertEquals("新标题", dto.getTitle());
        assertEquals("旧内容", dto.getContent());
        assertFalse(dto.getIsPinned());
    }

    @Test
    @Transactional
    void update_OnlyContent_UpdatesOnlyContent() {
        // Arrange
        User user = createTestUser();
        Announcement announcement = new Announcement("标题", "旧内容", user.getId());
        Long id = announcementRepository.saveAndFlush(announcement).getId();

        UpdateAnnouncementRequest request = new UpdateAnnouncementRequest();
        request.setContent("新内容");

        // Act
        AnnouncementDTO dto = announcementService.update(id, request);

        // Assert
        assertEquals("标题", dto.getTitle());
        assertEquals("新内容", dto.getContent());
    }

    @Test
    @Transactional
    void update_OnlyIsPinned_UpdatesOnlyIsPinned() {
        // Arrange
        User user = createTestUser();
        Announcement announcement = new Announcement("标题", "内容", user.getId());
        announcement.setIsPinned(false);
        Long id = announcementRepository.saveAndFlush(announcement).getId();

        UpdateAnnouncementRequest request = new UpdateAnnouncementRequest();
        request.setIsPinned(true);

        // Act
        AnnouncementDTO dto = announcementService.update(id, request);

        // Assert
        assertEquals("标题", dto.getTitle());
        assertEquals("内容", dto.getContent());
        assertTrue(dto.getIsPinned());
    }

    @Test
    void update_NonExistingAnnouncement_ThrowsResourceNotFoundException() {
        UpdateAnnouncementRequest request = new UpdateAnnouncementRequest();
        request.setTitle("新标题");

        Exception exception = assertThrows(ResourceNotFoundException.class,
                () -> announcementService.update(999L, request));
        assertEquals("公告不存在", exception.getMessage());
    }

    // ==================== delete ====================

    @Test
    @Transactional
    void delete_ExistingAnnouncement_RemovesFromDatabase() {
        // Arrange
        User user = createTestUser();
        Announcement announcement = new Announcement("待删除", "内容", user.getId());
        Long id = announcementRepository.saveAndFlush(announcement).getId();

        // Act
        announcementService.delete(id);

        // Assert
        assertFalse(announcementRepository.existsById(id));
    }

    @Test
    void delete_NonExistingAnnouncement_ThrowsResourceNotFoundException() {
        Exception exception = assertThrows(ResourceNotFoundException.class,
                () -> announcementService.delete(999L));
        assertEquals("公告不存在", exception.getMessage());
    }
}
