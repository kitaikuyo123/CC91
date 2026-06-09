package com.cc91.controller;

import com.cc91.entity.Announcement;
import com.cc91.entity.User;
import com.cc91.repository.AnnouncementRepository;
import com.cc91.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AnnouncementController HTTP endpoint tests
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AnnouncementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private User createTestUser(String username) {
        User user = new User(username, username + "@example.com", passwordEncoder.encode("password123"));
        return userRepository.save(user);
    }

    // ==================== GET /api/announcements ====================

    @Test
    @Transactional
    void getAllAnnouncements_Returns200() throws Exception {
        // Arrange
        User user = createTestUser("author1");
        Announcement a1 = new Announcement("公告1", "内容1", user.getId());
        a1.setIsPinned(false);
        announcementRepository.saveAndFlush(a1);

        Announcement a2 = new Announcement("置顶公告", "内容2", user.getId());
        a2.setIsPinned(true);
        announcementRepository.saveAndFlush(a2);

        // Act & Assert: 置顶优先
        mockMvc.perform(get("/api/announcements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("置顶公告"))
                .andExpect(jsonPath("$[0].isPinned").value(true))
                .andExpect(jsonPath("$[1].title").value("公告1"));
    }

    @Test
    void getAllAnnouncements_Empty_Returns200() throws Exception {
        mockMvc.perform(get("/api/announcements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==================== GET /api/announcements/{id} ====================

    @Test
    @Transactional
    void getAnnouncementById_Existing_Returns200() throws Exception {
        // Arrange
        User user = createTestUser("author1");
        Announcement announcement = new Announcement("测试公告", "测试内容", user.getId());
        announcement.setAuthor(user);
        announcement.setIsPinned(true);
        announcement = announcementRepository.saveAndFlush(announcement);

        // Act & Assert
        mockMvc.perform(get("/api/announcements/" + announcement.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(announcement.getId()))
                .andExpect(jsonPath("$.title").value("测试公告"))
                .andExpect(jsonPath("$.content").value("测试内容"))
                .andExpect(jsonPath("$.isPinned").value(true))
                .andExpect(jsonPath("$.authorUsername").value(user.getUsername()))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void getAnnouncementById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/announcements/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("公告不存在"));
    }

    // ==================== POST /api/admin/announcements ====================

    @Test
    @WithMockUser(username = "admin_user", roles = "ADMIN")
    @Transactional
    void createAnnouncement_ValidRequest_Returns200() throws Exception {
        // Arrange: 创建用户，username 必须与 @WithMockUser 一致
        createTestUser("admin_user");

        String requestBody = """
            {
                "title": "新公告",
                "content": "新内容"
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/admin/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("公告创建成功"))
                .andExpect(jsonPath("$.data.title").value("新公告"))
                .andExpect(jsonPath("$.data.content").value("新内容"))
                .andExpect(jsonPath("$.data.authorUsername").value("admin_user"))
                .andExpect(jsonPath("$.data.isPinned").value(false))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    @WithMockUser(username = "admin_user", roles = "ADMIN")
    @Transactional
    void createAnnouncement_WithIsPinned_Returns200() throws Exception {
        // Arrange
        createTestUser("admin_user");

        String requestBody = """
            {
                "title": "置顶公告",
                "content": "重要内容",
                "isPinned": true
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/admin/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("置顶公告"))
                .andExpect(jsonPath("$.data.isPinned").value(true));
    }

    @Test
    @WithMockUser(username = "admin_user", roles = "ADMIN")
    void createAnnouncement_ValidationError_Returns400() throws Exception {
        // Arrange: 缺少必填 title
        String requestBody = """
            {
                "title": "",
                "content": "内容"
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/admin/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("输入验证失败"));
    }

    @Test
    @WithMockUser(username = "regular_user")
    void createAnnouncement_NonAdmin_Returns403() throws Exception {
        String requestBody = """
            {
                "title": "新公告",
                "content": "新内容"
            }
            """;

        mockMvc.perform(post("/api/admin/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/admin/announcements/{id} ====================

    @Test
    @WithMockUser(username = "admin_user", roles = "ADMIN")
    @Transactional
    void updateAnnouncement_ValidRequest_Returns200() throws Exception {
        // Arrange
        User user = createTestUser("admin_user");
        Announcement announcement = new Announcement("旧标题", "旧内容", user.getId());
        announcement = announcementRepository.saveAndFlush(announcement);

        String requestBody = """
            {
                "title": "新标题",
                "content": "新内容",
                "isPinned": true
            }
            """;

        // Act & Assert
        mockMvc.perform(put("/api/admin/announcements/" + announcement.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("公告更新成功"))
                .andExpect(jsonPath("$.data.title").value("新标题"))
                .andExpect(jsonPath("$.data.content").value("新内容"))
                .andExpect(jsonPath("$.data.isPinned").value(true));
    }

    @Test
    @WithMockUser(username = "admin_user", roles = "ADMIN")
    @Transactional
    void updateAnnouncement_PartialUpdate_Returns200() throws Exception {
        // Arrange
        User user = createTestUser("admin_user");
        Announcement announcement = new Announcement("原标题", "原内容", user.getId());
        announcement.setIsPinned(false);
        Long id = announcementRepository.saveAndFlush(announcement).getId();

        String requestBody = """
            {
                "title": "新标题"
            }
            """;

        // Act & Assert
        mockMvc.perform(put("/api/admin/announcements/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("新标题"))
                .andExpect(jsonPath("$.data.content").value("原内容"))
                .andExpect(jsonPath("$.data.isPinned").value(false));
    }

    @Test
    @WithMockUser(username = "admin_user", roles = "ADMIN")
    void updateAnnouncement_NotFound_Returns404() throws Exception {
        String requestBody = """
            {
                "title": "新标题"
            }
            """;

        mockMvc.perform(put("/api/admin/announcements/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("公告不存在"));
    }

    @Test
    @WithMockUser(username = "regular_user")
    @Transactional
    void updateAnnouncement_NonAdmin_Returns403() throws Exception {
        // Arrange
        User user = createTestUser("regular_user");
        Announcement announcement = new Announcement("标题", "内容", user.getId());
        announcement = announcementRepository.saveAndFlush(announcement);

        String requestBody = """
            {
                "title": "新标题"
            }
            """;

        mockMvc.perform(put("/api/admin/announcements/" + announcement.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/admin/announcements/{id} ====================

    @Test
    @WithMockUser(username = "admin_user", roles = "ADMIN")
    @Transactional
    void deleteAnnouncement_Existing_Returns200() throws Exception {
        // Arrange
        User user = createTestUser("admin_user");
        Announcement announcement = new Announcement("待删除", "内容", user.getId());
        announcement = announcementRepository.saveAndFlush(announcement);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/announcements/" + announcement.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("公告删除成功"));
    }

    @Test
    @WithMockUser(username = "admin_user", roles = "ADMIN")
    void deleteAnnouncement_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/admin/announcements/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("公告不存在"));
    }

    @Test
    @WithMockUser
    @Transactional
    void deleteAnnouncement_NonAdmin_Returns403() throws Exception {
        // Arrange
        User user = createTestUser("user");
        Announcement announcement = new Announcement("标题", "内容", user.getId());
        announcement = announcementRepository.saveAndFlush(announcement);

        mockMvc.perform(delete("/api/admin/announcements/" + announcement.getId()))
                .andExpect(status().isForbidden());
    }
}
