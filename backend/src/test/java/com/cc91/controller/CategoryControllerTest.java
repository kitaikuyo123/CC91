package com.cc91.controller;

import com.cc91.entity.Category;
import com.cc91.repository.CategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CategoryController HTTP endpoint tests
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanDatabase() {
        categoryRepository.deleteAll();
    }

    // ==================== GET /api/categories ====================

    @Test
    @Transactional
    void getAllCategories_Returns200() throws Exception {
        // Arrange
        Category cat1 = new Category("Tech", "Desc1", 1);
        Category cat2 = new Category("Life", "Desc2", 2);
        categoryRepository.save(cat1);
        categoryRepository.save(cat2);

        // Act & Assert
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Tech"))
                .andExpect(jsonPath("$[1].name").value("Life"));
    }

    @Test
    void getAllCategories_Empty_Returns200() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==================== GET /api/categories/{id} ====================

    @Test
    @WithMockUser
    @Transactional
    void getCategoryById_Existing_Returns200() throws Exception {
        // Arrange
        Category category = new Category("Tech", "Desc", 1);
        category = categoryRepository.save(category);

        // Act & Assert
        mockMvc.perform(get("/api/categories/" + category.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(category.getId()))
                .andExpect(jsonPath("$.name").value("Tech"))
                .andExpect(jsonPath("$.description").value("Desc"))
                .andExpect(jsonPath("$.sortOrder").value(1));
    }

    @Test
    @WithMockUser
    void getCategoryById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/categories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("版块不存在"));
    }

    // ==================== POST /api/categories ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCategory_ValidRequest_Returns200() throws Exception {
        // Arrange
        String requestBody = """
            {
                "name": "Tech",
                "description": "Tech discussion",
                "sortOrder": 1
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("版块创建成功"))
                .andExpect(jsonPath("$.data.name").value("Tech"))
                .andExpect(jsonPath("$.data.description").value("Tech discussion"))
                .andExpect(jsonPath("$.data.sortOrder").value(1))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void createCategory_DuplicateName_Returns400() throws Exception {
        // Arrange
        Category existing = new Category("Tech", "Desc", 1);
        categoryRepository.save(existing);

        String requestBody = """
            {
                "name": "Tech",
                "description": "Duplicate",
                "sortOrder": 2
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("版块名称已存在"));
    }

    // ==================== PUT /api/categories/{id} ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void updateCategory_ValidRequest_Returns200() throws Exception {
        // Arrange
        Category category = new Category("Tech", "Old desc", 1);
        category = categoryRepository.save(category);

        String requestBody = """
            {
                "name": "Technology",
                "description": "New desc",
                "sortOrder": 2
            }
            """;

        // Act & Assert
        mockMvc.perform(put("/api/categories/" + category.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("版块更新成功"))
                .andExpect(jsonPath("$.data.name").value("Technology"))
                .andExpect(jsonPath("$.data.description").value("New desc"))
                .andExpect(jsonPath("$.data.sortOrder").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCategory_NotFound_Returns404() throws Exception {
        String requestBody = """
            {
                "name": "Tech",
                "description": "Desc"
            }
            """;

        mockMvc.perform(put("/api/categories/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("版块不存在"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void updateCategory_DuplicateName_Returns400() throws Exception {
        // Arrange
        Category cat1 = new Category("Cat1", "Desc1", 1);
        Category cat2 = new Category("Cat2", "Desc2", 2);
        cat1 = categoryRepository.save(cat1);
        categoryRepository.save(cat2);

        String requestBody = """
            {
                "name": "Cat2"
            }
            """;

        // Act & Assert
        mockMvc.perform(put("/api/categories/" + cat1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("版块名称已存在"));
    }

    // ==================== DELETE /api/categories/{id} ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void deleteCategory_Existing_Returns200() throws Exception {
        // Arrange
        Category category = new Category("Tech", "Desc", 1);
        category = categoryRepository.save(category);

        // Act & Assert
        mockMvc.perform(delete("/api/categories/" + category.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("版块删除成功"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCategory_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/categories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("版块不存在"));
    }
}
