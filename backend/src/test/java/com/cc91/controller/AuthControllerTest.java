package com.cc91.controller;

import com.cc91.entity.RefreshToken;
import com.cc91.entity.User;
import com.cc91.entity.VerificationCode;
import com.cc91.repository.RefreshTokenRepository;
import com.cc91.repository.UserRepository;
import com.cc91.repository.VerificationCodeRepository;
import com.cc91.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-controller-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "app.dev-data.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    @BeforeEach
    @Transactional
    void cleanDatabase() {
        verificationCodeRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void register_Success_Returns201() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "newuser",
                                "email": "new@example.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(AuthService.VERIFICATION_CODE_SENT))
                .andExpect(jsonPath("$.expiresIn").value(600));
    }

    @Test
    @Transactional
    void register_Conflicts_Return409() throws Exception {
        userRepository.save(new User("existinguser", "existing@example.com", passwordEncoder.encode("password123")));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "existinguser",
                                "email": "new@example.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(AuthService.USERNAME_ALREADY_USED));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "newuser",
                                "email": "existing@example.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(AuthService.EMAIL_ALREADY_REGISTERED));
    }

    @Test
    @Transactional
    void verifyEmail_Success_Returns200() throws Exception {
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        user.setIsLocked(true);
        userRepository.save(user);
        verificationCodeRepository.save(new VerificationCode("test@example.com", "123456", "REGISTER", LocalDateTime.now().plusMinutes(10)));

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "test@example.com",
                                "code": "123456"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified"));
    }

    @Test
    @Transactional
    void login_Success_ReturnsTokensAndUserInfo() throws Exception {
        userRepository.save(new User("testuser", "test@example.com", passwordEncoder.encode("password123")));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "testuser",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.expiresIn").exists());
    }

    @Test
    @Transactional
    void login_WrongPassword_Returns401() throws Exception {
        userRepository.save(new User("testuser", "test@example.com", passwordEncoder.encode("password123")));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "testuser",
                                "password": "wrongpassword"
                            }
                            """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(AuthService.BAD_CREDENTIALS));
    }

    @Test
    @Transactional
    void login_LockedAccount_Returns423() throws Exception {
        User user = new User("lockeduser", "locked@example.com", passwordEncoder.encode("password123"));
        user.setIsLocked(true);
        user.setLockUntil(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "lockeduser",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void health_Returns200() throws Exception {
        mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OK"));
    }

    @Test
    @Transactional
    void refresh_ValidToken_ReturnsRotatedTokens() throws Exception {
        User user = userRepository.save(new User("testuser", "test@example.com", passwordEncoder.encode("password123")));
        RefreshToken refreshToken = refreshTokenRepository.save(new RefreshToken(
                user.getId(),
                UUID.randomUUID().toString(),
                LocalDateTime.now().plusDays(7)
        ));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"refreshToken\":\"%s\"}", refreshToken.getToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.expiresIn").exists());
    }

    @Test
    void refresh_InvalidToken_Returns401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"invalid-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(AuthService.REFRESH_TOKEN_INVALID));
    }

    @Test
    @Transactional
    void logout_ValidToken_Returns200() throws Exception {
        User user = userRepository.save(new User("testuser", "test@example.com", passwordEncoder.encode("password123")));
        RefreshToken refreshToken = refreshTokenRepository.save(new RefreshToken(
                user.getId(),
                UUID.randomUUID().toString(),
                LocalDateTime.now().plusDays(7)
        ));

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"refreshToken\":\"%s\"}", refreshToken.getToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out"));
    }

    @Test
    @Transactional
    void forgotAndResetPassword_ReturnsExpectedResponses() throws Exception {
        userRepository.save(new User("testuser", "test@example.com", passwordEncoder.encode("password123")));
        verificationCodeRepository.save(new VerificationCode("test@example.com", "123456", "PASSWORD_RESET", LocalDateTime.now().plusMinutes(10)));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If the email is registered, a verification code has been sent"));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "test@example.com",
                                "code": "123456",
                                "newPassword": "newpassword123"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));
    }
}
