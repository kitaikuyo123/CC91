package com.cc91.service;

import com.cc91.dto.LoginRequest;
import com.cc91.dto.LoginResponse;
import com.cc91.dto.RegisterRequest;
import com.cc91.dto.RegisterResponse;
import com.cc91.dto.VerifyEmailRequest;
import com.cc91.entity.User;
import com.cc91.entity.VerificationCode;
import com.cc91.repository.UserRepository;
import com.cc91.repository.VerificationCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuthService 业务逻辑测试
 * 只测试业务路径，不测试框架行为
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        verificationCodeRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== register 方法测试 ====================

    @Test
    @Transactional
    void register_UsernameExists_ThrowsException() {
        // Arrange: 先创建一个用户
        User existingUser = new User("existinguser", "existing@example.com", passwordEncoder.encode("password123"));
        userRepository.save(existingUser);

        // Act & Assert: 尝试用相同用户名注册
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        Exception exception = assertThrows(RuntimeException.class, () -> authService.register(request));
        assertEquals("用户名已被使用", exception.getMessage());
    }

    @Test
    @Transactional
    void register_EmailExists_ThrowsException() {
        // Arrange: 先创建一个用户
        User existingUser = new User("user1", "existing@example.com", passwordEncoder.encode("password123"));
        userRepository.save(existingUser);

        // Act & Assert: 尝试用相同邮箱注册
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        Exception exception = assertThrows(RuntimeException.class, () -> authService.register(request));
        assertEquals("邮箱已被注册", exception.getMessage());
    }

    @Test
    @Transactional
    void register_Success_ReturnsVerificationCodeInfo() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        // Act
        RegisterResponse response = authService.register(request);

        // Assert: 验证响应
        assertNotNull(response);
        assertEquals("验证码已发送至邮箱", response.getMessage());
        assertEquals(600, response.getExpiresIn());

        // Assert: 验证验证码已保存到数据库
        VerificationCode savedCode = verificationCodeRepository
                .findFirstByEmailAndTypeOrderByCreatedAtDesc("new@example.com", "REGISTER")
                .orElse(null);
        assertNotNull(savedCode);
        assertFalse(savedCode.getUsed());
        assertEquals("REGISTER", savedCode.getType());
        assertNotNull(savedCode.getExpiresAt());
        assertTrue(savedCode.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    // ==================== verifyEmail 方法测试 ====================

    @Test
    @Transactional
    void verifyEmail_CodeNotExists_ThrowsException() {
        // Arrange
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("test@example.com");
        request.setCode("123456");

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> authService.verifyEmail(request));
        assertEquals("验证码不存在", exception.getMessage());
    }

    @Test
    @Transactional
    void verifyEmail_CodeAlreadyUsed_ThrowsException() {
        // Arrange: 创建一个已使用的验证码
        VerificationCode usedCode = new VerificationCode(
                "test@example.com",
                "123456",
                "REGISTER",
                LocalDateTime.now().plusMinutes(10)
        );
        usedCode.setUsed(true);
        verificationCodeRepository.save(usedCode);

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("test@example.com");
        request.setCode("123456");

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> authService.verifyEmail(request));
        assertEquals("验证码已使用", exception.getMessage());
    }

    @Test
    @Transactional
    void verifyEmail_CodeExpired_ThrowsException() {
        // Arrange: 创建一个已过期的验证码
        VerificationCode expiredCode = new VerificationCode(
                "test@example.com",
                "123456",
                "REGISTER",
                LocalDateTime.now().minusMinutes(1)  // 过期
        );
        verificationCodeRepository.save(expiredCode);

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("test@example.com");
        request.setCode("123456");

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> authService.verifyEmail(request));
        assertEquals("验证码已过期", exception.getMessage());
    }

    @Test
    @Transactional
    void verifyEmail_Success_MarksCodeAsUsed() {
        // Arrange: 创建一个有效的验证码
        VerificationCode validCode = new VerificationCode(
                "test@example.com",
                "123456",
                "REGISTER",
                LocalDateTime.now().plusMinutes(10)
        );
        verificationCodeRepository.save(validCode);

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("test@example.com");
        request.setCode("123456");

        // Act
        authService.verifyEmail(request);

        // Assert: 验证码被标记为已使用
        VerificationCode updatedCode = verificationCodeRepository
                .findByEmailAndCode("test@example.com", "123456")
                .orElse(null);
        assertNotNull(updatedCode);
        assertTrue(updatedCode.getUsed());
    }

    // ==================== login 方法测试 ====================

    @Test
    @Transactional
    void login_UserNotExists_ThrowsException() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password123");

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    @Transactional
    void login_AccountLocked_ThrowsException() {
        // Arrange: 创建一个被锁定的用户
        User lockedUser = new User("lockeduser", "locked@example.com", passwordEncoder.encode("password123"));
        lockedUser.setIsLocked(true);
        lockedUser.setLockUntil(LocalDateTime.now().plusMinutes(30));
        userRepository.save(lockedUser);

        LoginRequest request = new LoginRequest();
        request.setUsername("lockeduser");
        request.setPassword("password123");

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertTrue(exception.getMessage().contains("已锁定"));
    }

    @Test
    @Transactional
    void login_Success_ReturnsJwtAndResetsFailedAttempts() {
        // Arrange: 创建一个有失败记录的用户
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        user.setFailedLoginAttempts(3);
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        // Act
        LoginResponse response = authService.login(request);

        // Assert: 验证返回的 JWT
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertTrue(response.getExpiresIn() > 0);

        // Assert: 验证失败次数被重置
        User updatedUser = userRepository.findByUsername("testuser").orElse(null);
        assertNotNull(updatedUser);
        assertEquals(0, updatedUser.getFailedLoginAttempts());
        assertFalse(updatedUser.getIsLocked());
        assertNull(updatedUser.getLockUntil());
    }

    @Test
    @Transactional
    void login_WrongPassword_IncrementsFailedAttempts() {
        // Arrange
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        user.setFailedLoginAttempts(1);
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertEquals("用户名或密码错误", exception.getMessage());

        // Assert: 失败次数增加
        User updatedUser = userRepository.findByUsername("testuser").orElse(null);
        assertNotNull(updatedUser);
        assertEquals(2, updatedUser.getFailedLoginAttempts());
    }

    @Test
    @Transactional
    void login_FiveFailedAttempts_LocksAccount() {
        // Arrange
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        user.setFailedLoginAttempts(4);  // 已经失败4次
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertEquals("用户名或密码错误", exception.getMessage());

        // Assert: 账户被锁定
        User updatedUser = userRepository.findByUsername("testuser").orElse(null);
        assertNotNull(updatedUser);
        assertEquals(5, updatedUser.getFailedLoginAttempts());
        assertTrue(updatedUser.getIsLocked());
        assertNotNull(updatedUser.getLockUntil());
        assertTrue(updatedUser.getLockUntil().isAfter(LocalDateTime.now()));
    }
}
