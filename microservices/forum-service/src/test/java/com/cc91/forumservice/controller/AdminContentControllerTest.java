package com.cc91.forumservice.controller;

import com.cc91.forumservice.base.BaseWebMvcTest;
import com.cc91.forumservice.client.NotificationServiceClient;
import com.cc91.forumservice.client.UserServiceClient;
import com.cc91.forumservice.dto.ApiResponse;
import com.cc91.forumservice.dto.PostResponse;
import com.cc91.forumservice.dto.UpdatePostStatusRequest;
import com.cc91.forumservice.entity.Comment;
import com.cc91.forumservice.entity.Post;
import com.cc91.forumservice.exception.ResourceNotFoundException;
import com.cc91.forumservice.repository.CommentRepository;
import com.cc91.forumservice.repository.PostRepository;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminContentController @WebMvcTest.
 * Class-level @PreAuthorize("hasRole('ADMIN')") — non-admins must receive 403.
 */
@WebMvcTest(controllers = AdminContentController.class)
class AdminContentControllerTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private PostRepository postRepository;
    @MockBean private CommentRepository commentRepository;
    @MockBean private PostService postService;
    @MockBean private NotificationServiceClient notificationServiceClient;
    @MockBean private UserServiceClient userServiceClient;

    @Nested
    @DisplayName("Authorization (OWASP A01)")
    class Authorization {

        @Test
        @DisplayName("should return 401 when anonymous")
        void shouldReturn401WhenAnonymous() throws Exception {
            mockMvc.perform(get("/api/admin/posts"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 403 when role is USER")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn403WhenUser() throws Exception {
            mockMvc.perform(get("/api/admin/posts"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/posts")
    class GetPostsByStatus {

        @Test
        @DisplayName("ADMIN should get all posts (status filter optional)")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldReturn200WhenAdmin() throws Exception {
            PostResponse r = new PostResponse();
            r.setId(1L);
            r.setTitle("t");
            when(postService.getPostList(anyInt(), anyInt(), any()))
                    .thenReturn(new PageImpl<>(List.of(r), PageRequest.of(0, 1000), 1));
            mockMvc.perform(get("/api/admin/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1));
        }

        @Test
        @DisplayName("ADMIN should filter by status=DRAFT")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldFilterByDraft() throws Exception {
            when(postService.getPostList(anyInt(), anyInt(), eq("DRAFT")))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            mockMvc.perform(get("/api/admin/posts").param("status", "DRAFT"))
                    .andExpect(status().isOk());
            verify(postService).getPostList(anyInt(), anyInt(), eq("DRAFT"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/comments")
    class GetComments {

        @Test
        @DisplayName("ADMIN should list comments with post title JOIN")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldListComments() throws Exception {
            Comment c = new Comment(10L, 1L, "content", null);
            c.setId(1L);
            c.setStatus("PUBLISHED");
            c.setCreatedAt(LocalDateTime.now());
            when(commentRepository.findAllWithPostByOrderByCreatedAtDesc()).thenReturn(List.of(c));
            when(userServiceClient.getUsersByIds(any())).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/admin/comments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1));
        }

        @Test
        @DisplayName("should not fail when user-service batch call throws")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldNotFailWhenUserServiceDown() throws Exception {
            Comment c = new Comment(10L, 1L, "content", null);
            c.setId(1L);
            c.setStatus("PUBLISHED");
            c.setCreatedAt(LocalDateTime.now());
            when(commentRepository.findAllWithPostByOrderByCreatedAtDesc()).thenReturn(List.of(c));
            when(userServiceClient.getUsersByIds(any())).thenThrow(new RuntimeException("down"));

            mockMvc.perform(get("/api/admin/comments"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/posts/{id}/status")
    class UpdatePostStatus {

        @Test
        @DisplayName("ADMIN should update status (200)")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldUpdateStatus() throws Exception {
            Post p = new Post("t", "c", 1L);
            p.setId(1L);
            when(postRepository.findById(1L)).thenReturn(Optional.of(p));
            when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(put("/api/admin/posts/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdatePostStatusRequest("ARCHIVED"))))
                    .andExpect(status().isOk());

            // notification failure must not block the call (try/catch)
            verify(notificationServiceClient).createNotification(any());
        }

        @Test
        @DisplayName("should return 400 when status is blank")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldReturn400WhenStatusBlank() throws Exception {
            UpdatePostStatusRequest req = new UpdatePostStatusRequest();
            mockMvc.perform(put("/api/admin/posts/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 404 when post not found")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldReturn404WhenPostMissing() throws Exception {
            when(postRepository.findById(404L)).thenReturn(Optional.empty());
            mockMvc.perform(put("/api/admin/posts/404/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdatePostStatusRequest("X"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("notification failure should NOT abort status update")
        @WithMockUser(username = "root", roles = "ADMIN")
        void notificationFailureShouldNotAbort() throws Exception {
            Post p = new Post("t", "c", 1L);
            p.setId(1L);
            when(postRepository.findById(1L)).thenReturn(Optional.of(p));
            when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("notification down"))
                    .when(notificationServiceClient).createNotification(any());

            mockMvc.perform(put("/api/admin/posts/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdatePostStatusRequest("X"))))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/posts/{id} (force)")
    class ForceDeletePost {

        @Test
        @DisplayName("ADMIN should force-delete any post")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldForceDelete() throws Exception {
            Post p = new Post("t", "c", 1L);
            p.setId(1L);
            when(postRepository.findById(1L)).thenReturn(Optional.of(p));
            mockMvc.perform(delete("/api/admin/posts/1"))
                    .andExpect(status().isOk());
            verify(postRepository).delete(p);
        }

        @Test
        @DisplayName("should return 404 when post not found")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldReturn404WhenMissing() throws Exception {
            when(postRepository.findById(404L)).thenReturn(Optional.empty());
            mockMvc.perform(delete("/api/admin/posts/404"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/comments/{id} (force)")
    class ForceDeleteComment {

        @Test
        @DisplayName("ADMIN should force-delete any comment")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldForceDelete() throws Exception {
            Comment c = new Comment(10L, 1L, "c", null);
            c.setId(1L);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
            mockMvc.perform(delete("/api/admin/comments/1"))
                    .andExpect(status().isOk());
            verify(commentRepository).delete(c);
        }

        @Test
        @DisplayName("should return 404 when comment not found")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldReturn404WhenMissing() throws Exception {
            when(commentRepository.findById(404L)).thenReturn(Optional.empty());
            mockMvc.perform(delete("/api/admin/comments/404"))
                    .andExpect(status().isNotFound());
        }
    }
}
