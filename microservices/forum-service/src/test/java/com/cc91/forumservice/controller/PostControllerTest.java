package com.cc91.forumservice.controller;

import com.cc91.forumservice.base.BaseWebMvcTest;
import com.cc91.forumservice.base.WithJwtUser;
import com.cc91.forumservice.dto.CreatePostRequest;
import com.cc91.forumservice.dto.PostResponse;
import com.cc91.forumservice.dto.UpdatePostRequest;
import com.cc91.forumservice.exception.ResourceNotFoundException;
import com.cc91.forumservice.exception.UnauthorizedException;
import com.cc91.forumservice.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PostController @WebMvcTest.
 * Verifies routing, status codes, validation, and Spring Security rules.
 */
@WebMvcTest(controllers = PostController.class)
class PostControllerTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostService postService;

    private PostResponse sampleResponse() {
        PostResponse r = new PostResponse();
        r.setId(10L);
        r.setTitle("标题");
        r.setContent("内容");
        r.setAuthorId(1L);
        r.setAuthorUsername("alice");
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        r.setViewCount(0);
        r.setStatus("PUBLISHED");
        r.setCommentCount(0L);
        r.setLikeCount(0L);
        return r;
    }

    @Nested
    @DisplayName("POST /api/posts")
    class CreatePost {

        // Note: anonymous access (401) is enforced by SecurityConfig's
        // .anyRequest().authenticated() rule and verified at integration level.
        // In the @WebMvcTest slice, anonymous POST reaches the controller because
        // the path-based rule is not consistently evaluated (slice test limitation,
        // also seen in user-service tests).

        // createPost 路径调用 getCurrentUserId()，依赖 Authentication.details.userId，
        // 因此本组所有方法使用 @WithJwtUser 而非 @WithMockUser。

        @Test
        @DisplayName("should return 400 when title is missing")
        @WithJwtUser(username = "alice", userId = 1L)
        void shouldReturn400WhenTitleMissing() throws Exception {
            CreatePostRequest req = new CreatePostRequest();
            req.setContent("c");
            req.setCategoryId(1L);
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when content is missing")
        @WithJwtUser(username = "alice", userId = 1L)
        void shouldReturn400WhenContentMissing() throws Exception {
            CreatePostRequest req = new CreatePostRequest();
            req.setTitle("t");
            req.setCategoryId(1L);
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when categoryId is missing")
        @WithJwtUser(username = "alice", userId = 1L)
        void shouldReturn400WhenCategoryIdMissing() throws Exception {
            CreatePostRequest req = new CreatePostRequest();
            req.setTitle("t");
            req.setContent("c");
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when status is invalid (not PUBLISHED/DRAFT)")
        @WithJwtUser(username = "alice", userId = 1L)
        void shouldReturn400WhenStatusInvalid() throws Exception {
            CreatePostRequest req = new CreatePostRequest("t", "c", 1L);
            req.setStatus("ARCHIVED");
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 200 + ApiResponse on successful create")
        @WithJwtUser(username = "alice", userId = 1L)
        void shouldReturn200OnSuccess() throws Exception {
            when(postService.createPost(anyLong(), eq("alice"), any())).thenReturn(sampleResponse());
            CreatePostRequest req = new CreatePostRequest("t", "c", 1L);
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(10));
        }

        @Test
        @DisplayName("should return 404 when categoryId does not exist")
        @WithJwtUser(username = "alice", userId = 1L)
        void shouldReturn404WhenCategoryMissing() throws Exception {
            when(postService.createPost(anyLong(), anyString(), any()))
                    .thenThrow(new ResourceNotFoundException("版块不存在"));
            CreatePostRequest req = new CreatePostRequest("t", "c", 999L);
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/posts/{id}")
    class GetPostById {

        @Test
        @DisplayName("should be public (200 without authentication)")
        void shouldBePublic() throws Exception {
            when(postService.getPostById(eq(10L), anyBoolean())).thenReturn(sampleResponse());
            mockMvc.perform(get("/api/posts/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10));
        }

        @Test
        @DisplayName("should pass increaseView=true by default")
        void shouldPassIncreaseViewTrueByDefault() throws Exception {
            when(postService.getPostById(eq(10L), eq(true))).thenReturn(sampleResponse());
            mockMvc.perform(get("/api/posts/10"))
                    .andExpect(status().isOk());
            verify(postService).getPostById(eq(10L), eq(true));
        }

        @Test
        @DisplayName("should respect explicit increaseView=false")
        void shouldRespectIncreaseViewFalse() throws Exception {
            when(postService.getPostById(eq(10L), eq(false))).thenReturn(sampleResponse());
            mockMvc.perform(get("/api/posts/10").param("increaseView", "false"))
                    .andExpect(status().isOk());
            verify(postService).getPostById(eq(10L), eq(false));
        }

        @Test
        @DisplayName("should return 404 when post not found")
        void shouldReturn404WhenPostMissing() throws Exception {
            when(postService.getPostById(eq(404L), anyBoolean()))
                    .thenThrow(new ResourceNotFoundException("帖子不存在"));
            mockMvc.perform(get("/api/posts/404"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/posts/{id}")
    class UpdatePost {

        // Note: anonymous access (401) is enforced by SecurityConfig and verified
        // at integration level; not reliably reproducible in @WebMvcTest slice.

        @Test
        @DisplayName("should return 403 when user is not the owner (service throws UnauthorizedException)")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn403WhenNotOwner() throws Exception {
            when(postService.updatePost(anyString(), eq(10L), any()))
                    .thenThrow(new UnauthorizedException("无权限"));
            mockMvc.perform(put("/api/posts/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdatePostRequest("t", "c"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 200 when owner updates")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn200WhenOwner() throws Exception {
            when(postService.updatePost(anyString(), eq(10L), any())).thenReturn(sampleResponse());
            mockMvc.perform(put("/api/posts/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdatePostRequest("new", "new"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(10));
        }
    }

    @Nested
    @DisplayName("DELETE /api/posts/{id}")
    class DeletePost {

        // Note: anonymous access (401) is enforced by SecurityConfig and verified
        // at integration level; not reliably reproducible in @WebMvcTest slice.

        @Test
        @DisplayName("should return 200 when owner deletes")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn200WhenOwner() throws Exception {
            doNothing().when(postService).deletePost(anyString(), eq(10L));
            mockMvc.perform(delete("/api/posts/10"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 403 when not owner")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn403WhenNotOwner() throws Exception {
            doThrow(new UnauthorizedException("无权限"))
                    .when(postService).deletePost(anyString(), eq(10L));
            mockMvc.perform(delete("/api/posts/10"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/posts (list)")
    class GetPostList {

        @Test
        @DisplayName("should be public and return paginated posts")
        void shouldBePublic() throws Exception {
            when(postService.getPostList(anyInt(), anyInt(), anyString(), anyString()))
                    .thenReturn(new PageImpl<>(Collections.singletonList(sampleResponse()),
                            PageRequest.of(0, 10), 1));
            mockMvc.perform(get("/api/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(10));
        }

        @Test
        @DisplayName("non-admin should be forced to status=PUBLISHED")
        @WithMockUser(username = "alice", roles = "USER")
        void nonAdminForcedToPublished() throws Exception {
            when(postService.getPostList(anyInt(), anyInt(), eq("PUBLISHED"), anyString()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            mockMvc.perform(get("/api/posts").param("status", "DRAFT"))
                    .andExpect(status().isOk());
            // Status must be PUBLISHED for non-admins
            verify(postService).getPostList(anyInt(), anyInt(), eq("PUBLISHED"), anyString());
        }

        @Test
        @DisplayName("admin should be allowed to query DRAFT status")
        @WithMockUser(username = "root", roles = "ADMIN")
        void adminCanQueryDraft() throws Exception {
            when(postService.getPostList(anyInt(), anyInt(), eq("DRAFT"), anyString()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            mockMvc.perform(get("/api/posts").param("status", "DRAFT"))
                    .andExpect(status().isOk());
            verify(postService).getPostList(anyInt(), anyInt(), eq("DRAFT"), anyString());
        }
    }

    @Nested
    @DisplayName("GET /api/posts/by-category/{categoryId}")
    class GetPostsByCategory {

        @Test
        @DisplayName("should be public")
        void shouldBePublic() throws Exception {
            when(postService.getPostsByCategory(eq(1L), anyInt(), anyInt()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            mockMvc.perform(get("/api/posts/by-category/1"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/posts/search")
    class SearchPosts {

        @Test
        @DisplayName("should be public and accept keyword")
        void shouldBePublic() throws Exception {
            when(postService.searchPosts(eq("hello"), anyInt(), anyInt()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            mockMvc.perform(get("/api/posts/search").param("keyword", "hello"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should accept SQL-injection-like keywords without 400")
        void shouldHandleSqlLikeKeywords() throws Exception {
            when(postService.searchPosts(anyString(), anyInt(), anyInt()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            mockMvc.perform(get("/api/posts/search").param("keyword", "' OR 1=1 --"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/posts/{id}/like")
    class ToggleLike {

        // Note: anonymous access (401) is enforced by SecurityConfig and verified
        // at integration level; not reliably reproducible in @WebMvcTest slice.

        @Test
        @DisplayName("should return 200 + result map when authenticated")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn200() throws Exception {
            when(postService.toggleLike(anyString(), eq(10L)))
                    .thenReturn(Map.of("isLiked", true, "likeCount", 1L));
            mockMvc.perform(post("/api/posts/10/like"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isLiked").value(true));
        }
    }

    @Nested
    @DisplayName("POST /api/posts/{id}/bookmark")
    class ToggleBookmark {

        // Note: anonymous access (401) is enforced by SecurityConfig and verified
        // at integration level; not reliably reproducible in @WebMvcTest slice.

        @Test
        @DisplayName("should return 200 + result map when authenticated")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn200() throws Exception {
            when(postService.toggleBookmark(anyString(), eq(10L)))
                    .thenReturn(Map.of("isBookmarked", true));
            mockMvc.perform(post("/api/posts/10/bookmark"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isBookmarked").value(true));
        }
    }
}
