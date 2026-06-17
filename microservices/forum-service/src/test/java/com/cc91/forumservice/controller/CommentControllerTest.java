package com.cc91.forumservice.controller;

import com.cc91.forumservice.base.BaseWebMvcTest;
import com.cc91.forumservice.base.WithJwtUser;
import com.cc91.forumservice.dto.CommentResponse;
import com.cc91.forumservice.dto.CreateCommentRequest;
import com.cc91.forumservice.exception.ResourceNotFoundException;
import com.cc91.forumservice.exception.UnauthorizedException;
import com.cc91.forumservice.service.CommentService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CommentController @WebMvcTest.
 */
@WebMvcTest(controllers = CommentController.class)
class CommentControllerTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    private CommentResponse sample() {
        CommentResponse r = new CommentResponse();
        r.setId(1L);
        r.setPostId(10L);
        r.setAuthorId(1L);
        r.setAuthorUsername("alice");
        r.setContent("hi");
        r.setCreatedAt(LocalDateTime.now());
        r.setStatus("PUBLISHED");
        return r;
    }

    @Nested
    @DisplayName("POST /api/posts/{postId}/comments")
    class CreateComment {

        // Note: anonymous access (401) is enforced by SecurityConfig and verified
        // at integration level; not reliably reproducible in @WebMvcTest slice.

        // createComment 路径调用 getCurrentUserId()，本组所有方法用 @WithJwtUser。

        @Test
        @DisplayName("should return 400 when content is blank")
        @WithJwtUser(username = "alice", userId = 1L)
        void shouldReturn400WhenContentBlank() throws Exception {
            CreateCommentRequest req = new CreateCommentRequest();
            mockMvc.perform(post("/api/posts/10/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when content too long (>2000 chars)")
        @WithJwtUser(username = "alice", userId = 1L)
        void shouldReturn400WhenContentTooLong() throws Exception {
            CreateCommentRequest req = new CreateCommentRequest("x".repeat(2001));
            mockMvc.perform(post("/api/posts/10/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 200 on success")
        @WithJwtUser(username = "alice", userId = 1L)
        void shouldReturn200OnSuccess() throws Exception {
            when(commentService.createComment(anyLong(), anyString(), eq(10L), any())).thenReturn(sample());
            mockMvc.perform(post("/api/posts/10/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateCommentRequest("hi"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1));
        }

        @Test
        @DisplayName("should return 404 when post not found")
        @WithJwtUser(username = "alice", userId = 1L)
        void shouldReturn404WhenPostMissing() throws Exception {
            when(commentService.createComment(anyLong(), anyString(), eq(404L), any()))
                    .thenThrow(new ResourceNotFoundException("帖子不存在"));
            mockMvc.perform(post("/api/posts/404/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateCommentRequest("hi"))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/comments/{id}/reply")
    class ReplyComment {

        // Note: anonymous access (401) is enforced by SecurityConfig and verified
        // at integration level; not reliably reproducible in @WebMvcTest slice.

        @Test
        @DisplayName("should return 200 on success")
        @WithJwtUser(username = "alice", userId = 1L)
        void shouldReturn200OnSuccess() throws Exception {
            when(commentService.replyToComment(anyLong(), anyString(), eq(1L), any())).thenReturn(sample());
            mockMvc.perform(post("/api/comments/1/reply")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateCommentRequest("reply"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 404 when parent comment not found")
        @WithJwtUser(username = "alice", userId = 1L)
        void shouldReturn404WhenParentMissing() throws Exception {
            when(commentService.replyToComment(anyLong(), anyString(), eq(404L), any()))
                    .thenThrow(new ResourceNotFoundException("评论不存在"));
            mockMvc.perform(post("/api/comments/404/reply")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateCommentRequest("reply"))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/comments/{id}")
    class UpdateComment {

        // Note: anonymous access (401) is enforced by SecurityConfig and verified
        // at integration level; not reliably reproducible in @WebMvcTest slice.

        @Test
        @DisplayName("should return 403 when not owner")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn403WhenNotOwner() throws Exception {
            when(commentService.updateComment(anyString(), eq(1L), any()))
                    .thenThrow(new UnauthorizedException("无权限"));
            mockMvc.perform(put("/api/comments/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateCommentRequest("new"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 200 when owner updates")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn200WhenOwner() throws Exception {
            when(commentService.updateComment(anyString(), eq(1L), any())).thenReturn(sample());
            mockMvc.perform(put("/api/comments/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateCommentRequest("new"))))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/comments/{id}")
    class DeleteComment {

        // Note: anonymous access (401) is enforced by SecurityConfig and verified
        // at integration level; not reliably reproducible in @WebMvcTest slice.

        @Test
        @DisplayName("should return 200 when owner deletes")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn200WhenOwner() throws Exception {
            doNothing().when(commentService).deleteComment(anyString(), eq(1L));
            mockMvc.perform(delete("/api/comments/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 403 when not owner (IDOR)")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn403WhenNotOwner() throws Exception {
            doThrow(new UnauthorizedException("无权限"))
                    .when(commentService).deleteComment(anyString(), eq(1L));
            mockMvc.perform(delete("/api/comments/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/posts/{postId}/comments")
    class GetCommentsByPostId {

        @Test
        @DisplayName("should be public (no auth required)")
        void shouldBePublic() throws Exception {
            when(commentService.getCommentsByPostId(eq(10L)))
                    .thenReturn(Collections.singletonList(sample()));
            mockMvc.perform(get("/api/posts/10/comments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1));
        }

        @Test
        @DisplayName("should return empty array when no comments")
        void shouldReturnEmpty() throws Exception {
            when(commentService.getCommentsByPostId(eq(10L)))
                    .thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/posts/10/comments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }
}
