package com.cc91.userservice.controller;

import com.cc91.userservice.base.BaseWebMvcTest;
import com.cc91.userservice.dto.UserProfileDTO;
import com.cc91.userservice.exception.BadRequestException;
import com.cc91.userservice.exception.UnauthorizedException;
import com.cc91.userservice.service.UserService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
class UserControllerTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserProfileDTO profileDTO() {
        return new UserProfileDTO("alice", "a@b.com", "https://cdn/x.png",
                "bio", "SH", "https://alice.dev", LocalDateTime.now(), "USER");
    }

    @Nested
    @DisplayName("GET /api/users/me")
    class GetMe {

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenAnonymous() throws Exception {
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 200 + profile when authenticated")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn200WhenAuthenticated() throws Exception {
            when(userService.getMyProfile("alice")).thenReturn(profileDTO());
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("alice"))
                    .andExpect(jsonPath("$.email").value("a@b.com"));
        }
    }

    @Nested
    @DisplayName("GET /api/users/{username}")
    class GetByUsername {

        @Test
        @DisplayName("should be accessible without authentication (public path)")
        void shouldBePublic() throws Exception {
            when(userService.getUserProfile("alice")).thenReturn(profileDTO());
            mockMvc.perform(get("/api/users/alice"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("alice"));
        }

        @Test
        @DisplayName("should return 500 when service throws RuntimeException (GlobalExceptionHandler fallback)")
        void shouldReturnErrorWhenUserMissing() throws Exception {
            // UserService throws plain RuntimeException("用户不存在") — falls into GlobalExceptionHandler
            // RuntimeException handler changed from 400 to 500 (Task 1): uncaught RE = server bug.
            when(userService.getUserProfile("ghost"))
                    .thenThrow(new RuntimeException("用户不存在"));
            mockMvc.perform(get("/api/users/ghost"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("PUT /api/users/me/profile")
    class UpdateProfile {

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenAnonymous() throws Exception {
            mockMvc.perform(put("/api/users/me/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"bio\":\"hi\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 200 + updated profile when authenticated")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn200WhenAuthenticated() throws Exception {
            when(userService.updateProfile(eq("alice"), any())).thenReturn(profileDTO());
            mockMvc.perform(put("/api/users/me/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"bio\":\"new bio\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 400 when website is not a valid URL (validation)")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn400WhenWebsiteInvalid() throws Exception {
            mockMvc.perform(put("/api/users/me/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"website\":\"not-a-url\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when bio exceeds size limit")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn400WhenBioTooLong() throws Exception {
            String longBio = "x".repeat(501);
            mockMvc.perform(put("/api/users/me/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"bio\":\"" + longBio + "\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/users/me/password")
    class ChangePassword {

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenAnonymous() throws Exception {
            mockMvc.perform(put("/api/users/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"oldPassword\":\"a\",\"newPassword\":\"abcdef\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 200 when old password correct")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn200OnSuccess() throws Exception {
            doNothing().when(userService).changePassword(eq("alice"), any());
            mockMvc.perform(put("/api/users/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"oldPassword\":\"oldpass\",\"newPassword\":\"newpass123\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 400 when old password wrong (service throws BadRequestException)")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn400WhenOldWrong() throws Exception {
            doThrow(new BadRequestException("旧密码不正确"))
                    .when(userService).changePassword(eq("alice"), any());
            mockMvc.perform(put("/api/users/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"oldPassword\":\"wrong\",\"newPassword\":\"newpass123\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when new password too short (validation)")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn400WhenNewTooShort() throws Exception {
            mockMvc.perform(put("/api/users/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"oldPassword\":\"old\",\"newPassword\":\"abc\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when old password blank (validation)")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn400WhenOldBlank() throws Exception {
            mockMvc.perform(put("/api/users/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"oldPassword\":\"\",\"newPassword\":\"abcdef\"}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
