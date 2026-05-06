package com.cc91.controller;

import com.cc91.entity.Category;
import com.cc91.entity.User;
import com.cc91.repository.CategoryRepository;
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
 * AdminCategoryController HTTP endpoint tests
 * Tests admin-only category management endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ==================== POST /api/admin/categories ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createCategory_AsAdmin_Returns200() throws Exception {
        // Arrange
        String requestBody = """
            {
                "name": "Tech",
                "description": "Tech discussion",
                "sortOrder": 1
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("版块创建成功"))
                .andExpect(jsonPath("$.data.name").value("Tech"));
    }

    @Test
    @WithMockUser(username = "user")
    void createCategory_AsRegularUser_Returns403() throws Exception {
        // Arrange
        String requestBody = """
            {
                "name": "Tech",
                "description": "Desc",
                "sortOrder": 1
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCategory_AsAnonymous_Returns403() throws Exception {
        // Arrange
        String requestBody = """
            {
                "name": "Tech",
                "description": "Desc",
                "sortOrder": 1
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden()); // or 401 depending on config
    }

    // ==================== PUT /api/admin/categories/{id} ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Transactional
    void updateCategory_AsAdmin_Returns200() throws Exception {
        // Arrange
        Category category = new Category("Tech", "Old desc", 1);
        category = categoryRepository.save(category);

        String requestBody = """
            {
                "name": "Technology",
                "description": "New desc"
            }
            """;

        // Act & Assert
        mockMvc.perform(put("/api/admin/categories/" + category.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("版块更新成功"))
                .andExpect(jsonPath("$.data.name").value("Technology"));
    }

    @Test
    @WithMockUser(username = "user")
    @Transactional
    void updateCategory_AsRegularUser_Returns403() throws Exception {
        // Arrange
        Category category = new Category("Tech", "Desc", 1);
        category = categoryRepository.save(category);

        String requestBody = """
            {
                "name": "New Name"
            }
            """;

        // Act & Assert
        mockMvc.perform(put("/api/admin/categories/" + category.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/admin/categories/{id} ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Transactional
    void deleteCategory_AsAdmin_Returns200() throws Exception {
        // Arrange
        Category category = new Category("Tech", "Desc", 1);
        category = categoryRepository.save(category);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/categories/" + category.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("版块删除成功"));
    }

    @Test
    @WithMockUser(username = "user")
    @Transactional
    void deleteCategory_AsRegularUser_Returns403() throws Exception {
        // Arrange
        Category category = new Category("Tech", "Desc", 1);
        category = categoryRepository.save(category);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/categories/" + category.getId()))
                .andExpect(status().isForbidden());
    }
}
