package com.cc91.service;

import com.cc91.dto.CategoryDTO;
import com.cc91.dto.CreateCategoryRequest;
import com.cc91.dto.UpdateCategoryRequest;
import com.cc91.entity.Category;
import com.cc91.exception.ResourceNotFoundException;
import com.cc91.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CategoryService business logic tests
 */
@SpringBootTest
@ActiveProfiles("test")
class CategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanDatabase() {
        categoryRepository.deleteAll();
    }

    // ==================== findAll ====================

    @Test
    @Transactional
    void findAll_ReturnsCategoriesSortedBySortOrder() {
        // Arrange
        Category cat3 = new Category("Cat3", "Desc3", 3);
        Category cat1 = new Category("Cat1", "Desc1", 1);
        Category cat2 = new Category("Cat2", "Desc2", 2);
        categoryRepository.save(cat3);
        categoryRepository.save(cat1);
        categoryRepository.save(cat2);

        // Act
        List<CategoryDTO> categories = categoryService.findAll();

        // Assert
        assertNotNull(categories);
        assertEquals(3, categories.size());
        assertEquals("Cat1", categories.get(0).getName());
        assertEquals("Cat2", categories.get(1).getName());
        assertEquals("Cat3", categories.get(2).getName());
    }

    @Test
    void findAll_EmptyDatabase_ReturnsEmptyList() {
        // Act
        List<CategoryDTO> categories = categoryService.findAll();

        // Assert
        assertNotNull(categories);
        assertTrue(categories.isEmpty());
    }

    // ==================== findById ====================

    @Test
    @Transactional
    void findById_ExistingCategory_ReturnsCategoryDTO() {
        // Arrange
        Category category = new Category("Tech", "Tech discussion", 1);
        category = categoryRepository.save(category);

        // Act
        CategoryDTO dto = categoryService.findById(category.getId());

        // Assert
        assertNotNull(dto);
        assertEquals(category.getId(), dto.getId());
        assertEquals("Tech", dto.getName());
        assertEquals("Tech discussion", dto.getDescription());
        assertEquals(1, dto.getSortOrder());
        assertNotNull(dto.getCreatedAt());
    }

    @Test
    void findById_NonExistingCategory_ThrowsResourceNotFoundException() {
        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class,
                () -> categoryService.findById(999L));
        assertEquals("版块不存在", exception.getMessage());
    }

    // ==================== create ====================

    @Test
    @Transactional
    void create_ValidRequest_ReturnsCategoryDTO() {
        // Arrange
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Tech");
        request.setDescription("Tech discussion");
        request.setSortOrder(1);

        // Act
        CategoryDTO dto = categoryService.create(request);

        // Assert
        assertNotNull(dto);
        assertNotNull(dto.getId());
        assertEquals("Tech", dto.getName());
        assertEquals("Tech discussion", dto.getDescription());
        assertEquals(1, dto.getSortOrder());
        assertNotNull(dto.getCreatedAt());
    }

    @Test
    @Transactional
    void create_WithNullSortOrder_DefaultsToZero() {
        // Arrange
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Tech");
        request.setDescription("Desc");
        request.setSortOrder(null);

        // Act
        CategoryDTO dto = categoryService.create(request);

        // Assert
        assertEquals(0, dto.getSortOrder());
    }

    @Test
    @Transactional
    void create_DuplicateName_ThrowsIllegalArgumentException() {
        // Arrange
        Category existing = new Category("Tech", "Desc", 1);
        categoryRepository.save(existing);

        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Tech");
        request.setDescription("Another desc");
        request.setSortOrder(2);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> categoryService.create(request));
        assertEquals("版块名称已存在", exception.getMessage());
    }

    // ==================== update ====================

    @Test
    @Transactional
    void update_AllFields_ReturnsUpdatedCategoryDTO() {
        // Arrange
        Category category = new Category("Tech", "Old desc", 1);
        category = categoryRepository.save(category);

        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("Technology");
        request.setDescription("New desc");
        request.setSortOrder(2);

        // Act
        CategoryDTO dto = categoryService.update(category.getId(), request);

        // Assert
        assertNotNull(dto);
        assertEquals(category.getId(), dto.getId());
        assertEquals("Technology", dto.getName());
        assertEquals("New desc", dto.getDescription());
        assertEquals(2, dto.getSortOrder());
    }

    @Test
    @Transactional
    void update_OnlyDescription_UpdatesOnlyDescription() {
        // Arrange
        Category category = new Category("Tech", "Old desc", 1);
        Long id = categoryRepository.save(category).getId();

        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName(null);
        request.setDescription("New desc");
        request.setSortOrder(null);

        // Act
        CategoryDTO dto = categoryService.update(id, request);

        // Assert
        assertEquals("Tech", dto.getName());
        assertEquals("New desc", dto.getDescription());
        assertEquals(1, dto.getSortOrder());
    }

    @Test
    void update_NonExistingCategory_ThrowsResourceNotFoundException() {
        // Arrange
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("Tech");

        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class,
                () -> categoryService.update(999L, request));
        assertEquals("版块不存在", exception.getMessage());
    }

    @Test
    @Transactional
    void update_DuplicateName_ThrowsIllegalArgumentException() {
        // Arrange
        Category cat1 = new Category("Cat1", "Desc1", 1);
        Category cat2 = new Category("Cat2", "Desc2", 2);
        Long cat1Id = categoryRepository.save(cat1).getId();
        categoryRepository.save(cat2);

        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("Cat2");

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> categoryService.update(cat1Id, request));
        assertEquals("版块名称已存在", exception.getMessage());
    }

    @Test
    @Transactional
    void update_SameName_DoesNotThrowException() {
        // Arrange
        Category category = new Category("Tech", "Desc", 1);
        Long id = categoryRepository.save(category).getId();

        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("Tech");
        request.setDescription("New desc");

        // Act
        CategoryDTO dto = categoryService.update(id, request);

        // Assert
        assertEquals("Tech", dto.getName());
        assertEquals("New desc", dto.getDescription());
    }

    // ==================== delete ====================

    @Test
    @Transactional
    void delete_ExistingCategory_RemovesFromDatabase() {
        // Arrange
        Category category = new Category("Tech", "Desc", 1);
        Long id = categoryRepository.save(category).getId();

        // Act
        categoryService.delete(id);

        // Assert
        assertFalse(categoryRepository.existsById(id));
    }

    @Test
    void delete_NonExistingCategory_ThrowsResourceNotFoundException() {
        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class,
                () -> categoryService.delete(999L));
        assertEquals("版块不存在", exception.getMessage());
    }
}
