package com.cc91.userservice.controller;

import com.cc91.userservice.base.BaseWebMvcTest;
import com.cc91.userservice.dto.UserInfoDTO;
import com.cc91.userservice.entity.User;
import com.cc91.userservice.repository.UserRepository;
import com.cc91.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * InternalUserController @WebMvcTest.
 * /api/users/internal/** is permitAll but the InternalApiAuthFilter enforces
 * X-Internal-Token. The real filter is wired by SecurityConfig and reads
 * internal.token from @TestPropertySource in BaseWebMvcTest.
 */
@WebMvcTest(controllers = InternalUserController.class)
class InternalUserControllerTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    private static final String VALID_TOKEN = "test-internal-token-cc91";

    private UserInfoDTO info(long id, String name) {
        return new UserInfoDTO(id, name, "USER", null);
    }

    @Nested
    @DisplayName("X-Internal-Token enforcement (OWASP A01)")
    class TokenEnforcement {

        @Test
        @DisplayName("GET /api/users/internal/{id} should return 401 without X-Internal-Token")
        void shouldReturn401WithoutToken() throws Exception {
            mockMvc.perform(get("/api/users/internal/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/users/internal/{id} should return 401 with wrong X-Internal-Token")
        void shouldReturn401WithWrongToken() throws Exception {
            mockMvc.perform(get("/api/users/internal/1")
                            .header("X-Internal-Token", "wrong"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/users/internal/{id} should return 200 with valid X-Internal-Token")
        void shouldReturn200WithValidToken() throws Exception {
            when(userService.getUserInfoById(1L)).thenReturn(info(1L, "alice"));
            mockMvc.perform(get("/api/users/internal/1")
                            .header("X-Internal-Token", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.username").value("alice"));
        }
    }

    @Nested
    @DisplayName("Endpoints")
    class Endpoints {

        @Test
        @DisplayName("GET /{userId}/locked should return false when user does not exist")
        void lockedShouldReturnFalseWhenUserMissing() throws Exception {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            mockMvc.perform(get("/api/users/internal/99/locked")
                            .header("X-Internal-Token", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.locked").value(false));
        }

        @Test
        @DisplayName("GET /{userId}/locked should reflect user.isLocked")
        void lockedShouldReflectUserState() throws Exception {
            User u = new User("alice", "a@b.com", "h");
            u.setIsLocked(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(u));
            mockMvc.perform(get("/api/users/internal/1/locked")
                            .header("X-Internal-Token", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.locked").value(true));
        }

        @Test
        @DisplayName("GET /username/{username} should return user info")
        void getByUsernameShouldReturnInfo() throws Exception {
            when(userService.getUserInfoByUsername("alice")).thenReturn(info(1L, "alice"));
            mockMvc.perform(get("/api/users/internal/username/alice")
                            .header("X-Internal-Token", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("alice"));
        }

        @Test
        @DisplayName("POST /batch should resolve multiple users and skip missing")
        void batchShouldSkipMissing() throws Exception {
            when(userService.getUserInfoById(1L)).thenReturn(info(1L, "alice"));
            when(userService.getUserInfoById(2L)).thenThrow(new RuntimeException("not found"));
            when(userService.getUserInfoById(3L)).thenReturn(info(3L, "carol"));
            String body = "[1, 2, 3]";
            mockMvc.perform(post("/api/users/internal/batch")
                            .header("X-Internal-Token", VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[1].id").value(3));
        }

        @Test
        @DisplayName("PUT /{id}/avatar should call updateAvatarInternal")
        void updateAvatarShouldCallService() throws Exception {
            doNothing().when(userService).updateAvatarInternal(eq(1L), eq("https://cdn/x.png"));
            String body = "{\"avatarUrl\":\"https://cdn/x.png\"}";
            mockMvc.perform(put("/api/users/internal/1/avatar")
                            .header("X-Internal-Token", VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
            verify(userService).updateAvatarInternal(1L, "https://cdn/x.png");
        }
    }
}
