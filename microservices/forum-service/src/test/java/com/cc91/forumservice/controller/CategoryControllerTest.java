package com.cc91.forumservice.controller;

import com.cc91.forumservice.base.BaseWebMvcTest;
import com.cc91.forumservice.dto.CategoryDTO;
import com.cc91.forumservice.dto.CreateCategoryRequest;
import com.cc91.forumservice.dto.UpdateCategoryRequest;
import com.cc91.forumservice.exception.ResourceNotFoundException;
import com.cc91.forumservice.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CategoryController @WebMvcTest.
 * GET endpoints are public (per SecurityConfig whitelist), mutations require ADMIN.
 */
@WebMvcTest(controllers = CategoryController.class)
class CategoryControllerTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    private CategoryDTO sampleDTO() {
        return new CategoryDTO(1L, "Tech", "desc", 1, LocalDateTime.now(), 0L, 0L);
    }

    @Nested
    @DisplayName("GET /api/categories")
    class GetAll {

        @Test
        @DisplayName("should be public (no auth)")
        void shouldBePublic() throws Exception {
            when(categoryService.findAll()).thenReturn(List.of(sampleDTO()));
            mockMvc.perform(get("/api/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1));
        }

        @Test
        @DisplayName("should return empty array when no categories")
        void shouldReturnEmpty() throws Exception {
            when(categoryService.findAll()).thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/categories/{id}")
    class GetById {

        @Test
        @DisplayName("should be public")
        void shouldBePublic() throws Exception {
            when(categoryService.findById(1L)).thenReturn(sampleDTO());
            mockMvc.perform(get("/api/categories/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("should return 404 when category not found")
        void shouldReturn404WhenMissing() throws Exception {
            when(categoryService.findById(404L))
                    .thenThrow(new ResourceNotFoundException("版块不存在"));
            mockMvc.perform(get("/api/categories/404"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/categories (ADMIN)")
    class Create {

        @Test
        @DisplayName("should return 403 when not authenticated (class-level @PreAuthorize rejects anonymous)")
        void shouldReturn403WhenAnonymous() throws Exception {
            mockMvc.perform(post("/api/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateCategoryRequest("Tech", "d", 1))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 403 when role is USER")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn403WhenUser() throws Exception {
            mockMvc.perform(post("/api/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateCategoryRequest("Tech", "d", 1))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldReturn400WhenNameBlank() throws Exception {
            CreateCategoryRequest req = new CreateCategoryRequest();
            req.setDescription("desc");
            mockMvc.perform(post("/api/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 200 when ADMIN creates")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldReturn200WhenAdmin() throws Exception {
            when(categoryService.create(any())).thenReturn(sampleDTO());
            mockMvc.perform(post("/api/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateCategoryRequest("Tech", "d", 1))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1));
        }

        @Test
        @DisplayName("should return 400 when name already exists")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldReturn400WhenDuplicate() throws Exception {
            when(categoryService.create(any()))
                    .thenThrow(new IllegalArgumentException("版块名称已存在"));
            mockMvc.perform(post("/api/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateCategoryRequest("Tech", "d", 1))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/categories/{id} (ADMIN)")
    class Update {

        @Test
        @DisplayName("should return 403 when role is USER")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn403WhenUser() throws Exception {
            mockMvc.perform(put("/api/categories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateCategoryRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 200 when ADMIN updates")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldReturn200WhenAdmin() throws Exception {
            when(categoryService.update(eq(1L), any())).thenReturn(sampleDTO());
            UpdateCategoryRequest req = new UpdateCategoryRequest();
            req.setName("New");
            mockMvc.perform(put("/api/categories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 404 when category not found")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldReturn404WhenMissing() throws Exception {
            when(categoryService.update(eq(404L), any()))
                    .thenThrow(new ResourceNotFoundException("版块不存在"));
            mockMvc.perform(put("/api/categories/404")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateCategoryRequest())))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/categories/{id} (ADMIN)")
    class Delete {

        @Test
        @DisplayName("should return 403 when role is USER")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn403WhenUser() throws Exception {
            mockMvc.perform(delete("/api/categories/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 200 when ADMIN deletes empty category")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldReturn200WhenAdmin() throws Exception {
            doNothing().when(categoryService).delete(1L);
            mockMvc.perform(delete("/api/categories/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 400 when category has posts")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldReturn400WhenCategoryHasPosts() throws Exception {
            doThrow(new IllegalStateException("该版块下还有 5 篇帖子"))
                    .when(categoryService).delete(1L);
            mockMvc.perform(delete("/api/categories/1"))
                    .andExpect(status().isBadRequest());
        }
    }
}
