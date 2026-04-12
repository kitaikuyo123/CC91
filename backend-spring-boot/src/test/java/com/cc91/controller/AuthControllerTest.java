package com.cc91.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 业务逻辑测试
 * 只测试 Controller 特有的异常处理和 HTTP 状态码映射
 * 不测试 @Valid 注解的框架行为
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.cc91.repository.UserRepository userRepository;

    @BeforeEach
    @Transactional
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    // ==================== register 接口测试 ====================

    @Test
    void register_Success_Returns201() throws Exception {
        // Arrange
        String requestBody = """
            {
                "username": "newuser",
                "email": "new@example.com",
                "password": "password123"
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("验证码已发送至邮箱"))
                .andExpect(jsonPath("$.expiresIn").value(600));
    }

    @Test
    @Transactional
    void register_UsernameConflict_Returns409() throws Exception {
        // Arrange: 先创建一个用户
        com.cc91.entity.User existingUser = new com.cc91.entity.User(
                "existinguser",
                "existing@example.com",
                passwordEncoder.encode("password123")
        );
        userRepository.save(existingUser);

        String requestBody = """
            {
                "username": "existinguser",
                "email": "new@example.com",
                "password": "password123"
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("用户名已被使用"));
    }

    @Test
    @Transactional
    void register_EmailConflict_Returns409() throws Exception {
        // Arrange: 先创建一个用户
        com.cc91.entity.User existingUser = new com.cc91.entity.User(
                "user1",
                "existing@example.com",
                passwordEncoder.encode("password123")
        );
        userRepository.save(existingUser);

        String requestBody = """
            {
                "username": "newuser",
                "email": "existing@example.com",
                "password": "password123"
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("邮箱已被注册"));
    }

    // ==================== login 接口测试 ====================

    @Test
    @Transactional
    void login_Success_Returns200() throws Exception {
        // Arrange: 创建一个用户
        com.cc91.entity.User user = new com.cc91.entity.User(
                "testuser",
                "test@example.com",
                passwordEncoder.encode("password123")
        );
        userRepository.save(user);

        String requestBody = """
            {
                "username": "testuser",
                "password": "password123"
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.expiresIn").exists());
    }

    @Test
    @Transactional
    void login_Unauthorized_Returns401() throws Exception {
        // Arrange: 创建一个用户
        com.cc91.entity.User user = new com.cc91.entity.User(
                "testuser",
                "test@example.com",
                passwordEncoder.encode("password123")
        );
        userRepository.save(user);

        String requestBody = """
            {
                "username": "testuser",
                "password": "wrongpassword"
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @Transactional
    void login_AccountLocked_Returns423() throws Exception {
        // Arrange: 创建一个被锁定的用户
        com.cc91.entity.User lockedUser = new com.cc91.entity.User(
                "lockeduser",
                "locked@example.com",
                passwordEncoder.encode("password123")
        );
        lockedUser.setIsLocked(true);
        lockedUser.setLockUntil(java.time.LocalDateTime.now().plusMinutes(30));
        userRepository.save(lockedUser);

        String requestBody = """
            {
                "username": "lockeduser",
                "password": "password123"
            }
            """;

        // Act & Assert
        // 当账户锁定时，Controller 返回 423 状态码
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.message").exists());
    }

    // ==================== health 接口测试 ====================

    @Test
    void health_Returns200() throws Exception {
        mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OK"));
    }
}
