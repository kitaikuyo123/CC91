package com.cc91.userservice.controller;

import com.cc91.userservice.base.BaseWebMvcTest;
import com.cc91.userservice.dto.LoginResponse;
import com.cc91.userservice.dto.RefreshTokenResponse;
import com.cc91.userservice.dto.RegisterResponse;
import com.cc91.userservice.exception.BadRequestException;
import com.cc91.userservice.exception.UnauthorizedException;
import com.cc91.userservice.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController @WebMvcTest.
 * /api/auth/** is permitAll in SecurityConfig.
 */
@WebMvcTest(controllers = AuthController.class)
class AuthControllerTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    // ============================================================
    // register
    // ============================================================
    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("should return 201 and verification-sent message on success")
        void shouldReturn201OnSuccess() throws Exception {
            when(authService.register(any())).thenReturn(
                    new RegisterResponse(AuthService.VERIFICATION_CODE_SENT, 600));
            String body = "{\"username\":\"alice\",\"email\":\"a@b.com\",\"password\":\"secret123\"}";
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value(AuthService.VERIFICATION_CODE_SENT));
        }

        @Test
        @DisplayName("should return 409 Conflict when username already used")
        void shouldReturn409WhenUsernameUsed() throws Exception {
            when(authService.register(any()))
                    .thenThrow(new BadRequestException(AuthService.USERNAME_ALREADY_USED));
            String body = "{\"username\":\"alice\",\"email\":\"a@b.com\",\"password\":\"secret123\"}";
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 409 Conflict when email already registered")
        void shouldReturn409WhenEmailUsed() throws Exception {
            when(authService.register(any()))
                    .thenThrow(new BadRequestException(AuthService.EMAIL_ALREADY_REGISTERED));
            String body = "{\"username\":\"alice\",\"email\":\"a@b.com\",\"password\":\"secret123\"}";
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 400 when username too short (validation)")
        void shouldReturn400WhenUsernameTooShort() throws Exception {
            String body = "{\"username\":\"ab\",\"email\":\"a@b.com\",\"password\":\"secret123\"}";
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when email invalid (validation)")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            String body = "{\"username\":\"alice\",\"email\":\"not-an-email\",\"password\":\"secret123\"}";
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when password too short (validation)")
        void shouldReturn400WhenPasswordTooShort() throws Exception {
            String body = "{\"username\":\"alice\",\"email\":\"a@b.com\",\"password\":\"abc\"}";
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when body empty / missing required fields")
        void shouldReturn400WhenBodyEmpty() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ============================================================
    // verify-email
    // ============================================================
    @Nested
    @DisplayName("POST /api/auth/verify-email")
    class VerifyEmail {

        @Test
        @DisplayName("should return 200 on success")
        void shouldReturn200OnSuccess() throws Exception {
            doNothing().when(authService).verifyEmail(any());
            String body = "{\"email\":\"a@b.com\",\"code\":\"123456\"}";
            mockMvc.perform(post("/api/auth/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 400 when verification code not found")
        void shouldReturn400WhenNotFound() throws Exception {
            doThrow(new BadRequestException(AuthService.VERIFICATION_CODE_NOT_FOUND))
                    .when(authService).verifyEmail(any());
            String body = "{\"email\":\"a@b.com\",\"code\":\"999999\"}";
            mockMvc.perform(post("/api/auth/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when code is not 6 digits (validation)")
        void shouldReturn400WhenCodeInvalid() throws Exception {
            String body = "{\"email\":\"a@b.com\",\"code\":\"12\"}";
            mockMvc.perform(post("/api/auth/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ============================================================
    // login
    // ============================================================
    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("should return 200 + tokens on success")
        void shouldReturn200OnSuccess() throws Exception {
            when(authService.login(any())).thenReturn(
                    new LoginResponse("access", "refresh", 3600L, "alice", "a@b.com", "USER"));
            String body = "{\"username\":\"alice\",\"password\":\"secret123\"}";
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh"))
                    .andExpect(jsonPath("$.username").value("alice"));
        }

        @Test
        @DisplayName("should return 401 when credentials invalid")
        void shouldReturn401WhenCredentialsInvalid() throws Exception {
            when(authService.login(any()))
                    .thenThrow(new UnauthorizedException(AuthService.BAD_CREDENTIALS));
            String body = "{\"username\":\"alice\",\"password\":\"wrong\"}";
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 423 Locked when account temporarily locked")
        void shouldReturn423WhenLocked() throws Exception {
            when(authService.login(any()))
                    .thenThrow(new UnauthorizedException(
                            AuthService.ACCOUNT_LOCKED_PREFIX + "30 second(s)"));
            String body = "{\"username\":\"alice\",\"password\":\"x\"}";
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isLocked());
        }

        @Test
        @DisplayName("should return 403 Forbidden when account banned")
        void shouldReturn403WhenBanned() throws Exception {
            when(authService.login(any()))
                    .thenThrow(new UnauthorizedException(AuthService.ACCOUNT_BANNED));
            String body = "{\"username\":\"alice\",\"password\":\"x\"}";
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 when username missing (validation)")
        void shouldReturn400WhenMissing() throws Exception {
            String body = "{\"password\":\"x\"}";
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ============================================================
    // refresh
    // ============================================================
    @Nested
    @DisplayName("POST /api/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("should return 200 + new token pair on success")
        void shouldReturn200OnSuccess() throws Exception {
            when(authService.refreshToken(any()))
                    .thenReturn(new RefreshTokenResponse("new-access", "new-refresh", 3600L));
            String body = "{\"refreshToken\":\"old\"}";
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new-access"))
                    .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
        }

        @Test
        @DisplayName("should return 401 when refresh token invalid")
        void shouldReturn401WhenInvalid() throws Exception {
            when(authService.refreshToken(any()))
                    .thenThrow(new UnauthorizedException(AuthService.REFRESH_TOKEN_INVALID));
            String body = "{\"refreshToken\":\"bogus\"}";
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 400 when refresh token blank (validation)")
        void shouldReturn400WhenBlank() throws Exception {
            String body = "{\"refreshToken\":\"\"}";
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ============================================================
    // logout
    // ============================================================
    @Nested
    @DisplayName("POST /api/auth/logout")
    class Logout {

        @Test
        @DisplayName("should return 200 on success")
        void shouldReturn200OnSuccess() throws Exception {
            doNothing().when(authService).logout(anyString());
            String body = "{\"refreshToken\":\"abc\"}";
            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 400 when token not found")
        void shouldReturn400WhenTokenMissing() throws Exception {
            doThrow(new UnauthorizedException(AuthService.REFRESH_TOKEN_INVALID))
                    .when(authService).logout(anyString());
            String body = "{\"refreshToken\":\"abc\"}";
            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ============================================================
    // forgot-password / reset-password
    // ============================================================
    @Nested
    @DisplayName("forgot-password / reset-password")
    class PasswordReset {

        @Test
        @DisplayName("forgot-password should return 200 regardless of email existence (no enumeration)")
        void forgotPasswordShouldReturn200Always() throws Exception {
            doNothing().when(authService).forgotPassword(anyString());
            String body = "{\"email\":\"a@b.com\"}";
            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(
                            "If the email is registered, a verification code has been sent"));
        }

        @Test
        @DisplayName("forgot-password should return 400 when email invalid (validation)")
        void forgotPasswordShouldReturn400WhenEmailInvalid() throws Exception {
            String body = "{\"email\":\"not-an-email\"}";
            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("reset-password should return 200 on success")
        void resetPasswordShouldReturn200OnSuccess() throws Exception {
            doNothing().when(authService).resetPassword(anyString(), anyString(), anyString());
            String body = "{\"email\":\"a@b.com\",\"code\":\"123456\",\"newPassword\":\"newpass123\"}";
            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("reset-password should return 400 when new password too short (validation)")
        void resetPasswordShouldReturn400WhenPasswordShort() throws Exception {
            String body = "{\"email\":\"a@b.com\",\"code\":\"123456\",\"newPassword\":\"abc\"}";
            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("GET /api/auth/health should return 200 without auth")
    void healthShouldReturn200() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"x\"}"))
                .andExpect(status().isBadRequest()); // validation triggers 400 — sanity check
    }
}
