package com.cc91.forumservice.service;

import com.cc91.forumservice.dto.CategoryDTO;
import com.cc91.forumservice.dto.CreateCategoryRequest;
import com.cc91.forumservice.dto.UpdateCategoryRequest;
import com.cc91.forumservice.entity.Category;
import com.cc91.forumservice.exception.ResourceNotFoundException;
import com.cc91.forumservice.repository.CategoryRepository;
import com.cc91.forumservice.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CategoryService unit tests.
 * Covers findAll (with stats), findById, create (uniqueness), update
 * (uniqueness excluding self), delete (block when posts exist).
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private PostRepository postRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category(long id, String name, int sortOrder) {
        Category c = new Category(name, "desc", sortOrder);
        c.setId(id);
        c.setCreatedAt(LocalDateTime.now());
        return c;
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return empty list when no categories")
        void shouldReturnEmpty() {
            when(categoryRepository.findAllByOrderBySortOrderAsc()).thenReturn(Collections.emptyList());
            when(postRepository.countStatsByCategory(eq("PUBLISHED"), any()))
                    .thenReturn(Collections.emptyList());
            assertTrue(categoryService.findAll().isEmpty());
        }

        @Test
        @DisplayName("should return categories with post counts joined from stats query")
        void shouldReturnWithStats() {
            Category tech = category(1L, "Tech", 1);
            Category news = category(2L, "News", 2);
            when(categoryRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(tech, news));
            when(postRepository.countStatsByCategory(eq("PUBLISHED"), any()))
                    .thenReturn(List.of(
                            new Object[]{1L, 10L, 3L},
                            new Object[]{2L, 5L, 0L}
                    ));

            List<CategoryDTO> result = categoryService.findAll();
            assertEquals(2, result.size());
            assertEquals("Tech", result.get(0).getName());
            assertEquals(10L, result.get(0).getPostCount());
            assertEquals(3L, result.get(0).getTodayPostCount());
            assertEquals("News", result.get(1).getName());
            assertEquals(5L, result.get(1).getPostCount());
        }

        @Test
        @DisplayName("should default post counts to 0 when category has no stats row")
        void shouldDefaultToZeroWhenStatsMissing() {
            Category tech = category(1L, "Tech", 1);
            when(categoryRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(tech));
            when(postRepository.countStatsByCategory(eq("PUBLISHED"), any()))
                    .thenReturn(Collections.emptyList());

            List<CategoryDTO> result = categoryService.findAll();
            assertEquals(0L, result.get(0).getPostCount());
            assertEquals(0L, result.get(0).getTodayPostCount());
        }

        @Test
        @DisplayName("should respect sort order from repository")
        void shouldRespectSortOrder() {
            Category b = category(2L, "B", 2);
            Category a = category(1L, "A", 1);
            when(categoryRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(a, b));
            when(postRepository.countStatsByCategory(eq("PUBLISHED"), any()))
                    .thenReturn(Collections.emptyList());

            List<CategoryDTO> result = categoryService.findAll();
            assertEquals("A", result.get(0).getName());
            assertEquals("B", result.get(1).getName());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should throw ResourceNotFoundException when category not found")
        void shouldThrowWhenMissing() {
            when(categoryRepository.findById(404L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> categoryService.findById(404L));
        }

        @Test
        @DisplayName("should return DTO with post + today counts")
        void shouldReturnDtoWithCounts() {
            Category c = category(1L, "Tech", 1);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(c));
            when(postRepository.countByCategoryIdAndStatus(1L, "PUBLISHED")).thenReturn(7L);
            when(postRepository.countByCategoryIdAndStatusAndCreatedAtAfter(eq(1L), eq("PUBLISHED"), any()))
                    .thenReturn(2L);

            CategoryDTO result = categoryService.findById(1L);
            assertEquals("Tech", result.getName());
            assertEquals(7L, result.getPostCount());
            assertEquals(2L, result.getTodayPostCount());
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should throw IllegalArgumentException when name already exists")
        void shouldThrowWhenNameExists() {
            when(categoryRepository.findByName("Tech")).thenReturn(Optional.of(category(1L, "Tech", 0)));
            CreateCategoryRequest req = new CreateCategoryRequest("Tech", "desc", 0);
            assertThrows(IllegalArgumentException.class, () -> categoryService.create(req));
        }

        @Test
        @DisplayName("should create with default sortOrder when not provided")
        void shouldDefaultSortOrderToZero() {
            when(categoryRepository.findByName("Tech")).thenReturn(Optional.empty());
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

            CreateCategoryRequest req = new CreateCategoryRequest();
            req.setName("Tech");
            categoryService.create(req);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            assertEquals(0, captor.getValue().getSortOrder());
        }

        @Test
        @DisplayName("should create with provided sortOrder and description")
        void shouldCreateWithProvidedFields() {
            when(categoryRepository.findByName("News")).thenReturn(Optional.empty());
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

            CreateCategoryRequest req = new CreateCategoryRequest("News", "News desc", 5);
            CategoryDTO result = categoryService.create(req);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            assertEquals("News", captor.getValue().getName());
            assertEquals("News desc", captor.getValue().getDescription());
            assertEquals(5, captor.getValue().getSortOrder());
            assertEquals(0L, result.getPostCount());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should throw ResourceNotFoundException when category not found")
        void shouldThrowWhenMissing() {
            when(categoryRepository.findById(404L)).thenReturn(Optional.empty());
            UpdateCategoryRequest req = new UpdateCategoryRequest();
            assertThrows(ResourceNotFoundException.class, () -> categoryService.update(404L, req));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when new name collides with another category")
        void shouldThrowWhenNameCollides() {
            Category existing = category(1L, "Tech", 0);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(categoryRepository.findByName("News")).thenReturn(Optional.of(category(2L, "News", 0)));

            UpdateCategoryRequest req = new UpdateCategoryRequest();
            req.setName("News");
            assertThrows(IllegalArgumentException.class, () -> categoryService.update(1L, req));
        }

        @Test
        @DisplayName("should allow keeping the same name (no collision check against self)")
        void shouldAllowKeepingSameName() {
            Category existing = category(1L, "Tech", 0);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateCategoryRequest req = new UpdateCategoryRequest();
            req.setName("Tech"); // same name
            // Should not invoke findByName (no collision check on same name)
            assertDoesNotThrow(() -> categoryService.update(1L, req));
        }

        @Test
        @DisplayName("should update description and sortOrder when provided")
        void shouldUpdateDescriptionAndSortOrder() {
            Category existing = category(1L, "Tech", 0);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateCategoryRequest req = new UpdateCategoryRequest();
            req.setDescription("New desc");
            req.setSortOrder(99);
            categoryService.update(1L, req);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            assertEquals("New desc", captor.getValue().getDescription());
            assertEquals(99, captor.getValue().getSortOrder());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should throw ResourceNotFoundException when category not found")
        void shouldThrowWhenMissing() {
            when(categoryRepository.findById(404L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> categoryService.delete(404L));
        }

        @Test
        @DisplayName("should throw IllegalStateException when category has posts")
        void shouldThrowWhenCategoryHasPosts() {
            Category existing = category(1L, "Tech", 0);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(postRepository.countByCategoryId(1L)).thenReturn(5L);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> categoryService.delete(1L));
            assertTrue(ex.getMessage().contains("5"));
            verify(categoryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should delete when category has no posts")
        void shouldDeleteWhenEmpty() {
            Category existing = category(1L, "Tech", 0);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(postRepository.countByCategoryId(1L)).thenReturn(0L);

            categoryService.delete(1L);
            verify(categoryRepository).delete(existing);
        }
    }
}
