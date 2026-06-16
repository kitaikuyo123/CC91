package com.cc91.forumservice.controller;

import com.cc91.forumservice.base.BaseWebMvcTest;
import com.cc91.forumservice.dto.PostResponse;
import com.cc91.forumservice.dto.UserCommentResponse;
import com.cc91.forumservice.service.CommentService;
import com.cc91.forumservice.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
 * UserContentController @WebMvcTest.
 * All endpoints require authentication (no public whitelist).
 */
@WebMvcTest(controllers = UserContentController.class)
class UserContentControllerTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private PostService postService;
    @MockBean private CommentService commentService;

    private PostResponse samplePost() {
        PostResponse r = new PostResponse();
        r.setId(1L);
        r.setTitle("t");
        r.setContent("c");
        return r;
    }

    @Nested
    @DisplayName("GET /api/users/me/posts")
    class GetMyPosts {

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenAnonymous() throws Exception {
            mockMvc.perform(get("/api/users/me/posts"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 200 + posts when authenticated")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn200WhenAuthenticated() throws Exception {
            when(postService.getMyPosts("alice")).thenReturn(List.of(samplePost()));
            mockMvc.perform(get("/api/users/me/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1));
        }

        @Test
        @DisplayName("should return empty array when no posts")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturnEmpty() throws Exception {
            when(postService.getMyPosts("alice")).thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/users/me/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/users/me/drafts")
    class GetMyDrafts {

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenAnonymous() throws Exception {
            mockMvc.perform(get("/api/users/me/drafts"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return drafts when authenticated")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturnDrafts() throws Exception {
            when(postService.getMyDrafts(anyString())).thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/users/me/drafts"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/users/me/comments")
    class GetMyComments {

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenAnonymous() throws Exception {
            mockMvc.perform(get("/api/users/me/comments"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return comments when authenticated")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturnComments() throws Exception {
            UserCommentResponse r = new UserCommentResponse(
                    1L, 10L, "Post title", "content", null, LocalDateTime.now(), "PUBLISHED");
            when(commentService.getMyComments(anyString())).thenReturn(List.of(r));
            mockMvc.perform(get("/api/users/me/comments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/users/me/bookmarks")
    class GetMyBookmarks {

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenAnonymous() throws Exception {
            mockMvc.perform(get("/api/users/me/bookmarks"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return bookmarks when authenticated")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturnBookmarks() throws Exception {
            when(postService.getMyBookmarks(anyString())).thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/users/me/bookmarks"))
                    .andExpect(status().isOk());
        }
    }
}
