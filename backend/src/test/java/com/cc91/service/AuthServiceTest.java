package com.cc91.service;

import com.cc91.dto.LoginRequest;
import com.cc91.dto.LoginResponse;
import com.cc91.dto.RegisterRequest;
import com.cc91.dto.RegisterResponse;
import com.cc91.dto.VerifyEmailRequest;
import com.cc91.dto.RefreshTokenRequest;
import com.cc91.entity.User;
import com.cc91.entity.VerificationCode;
import com.cc91.entity.RefreshToken;
import com.cc91.repository.UserRepository;
import com.cc91.repository.VerificationCodeRepository;
import com.cc91.repository.RefreshTokenRepository;
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
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
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

    // ==================== refreshToken 方法测试 ====================

    @Test
    @Transactional
    void refreshToken_ValidToken_ReturnsNewTokens() {
        // Arrange: 创建用户和刷新令牌
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        userRepository.save(user);

        RefreshToken refreshToken = authService.createRefreshToken(user.getId());

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken.getToken());

        // Act
        com.cc91.dto.RefreshTokenResponse response = authService.refreshToken(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertNotEquals(refreshToken.getToken(), response.getRefreshToken()); // 新的 refresh token

        // Assert: 旧 token 被撤销
        RefreshToken oldToken = refreshTokenRepository.findByToken(refreshToken.getToken()).orElse(null);
        assertNotNull(oldToken);
        assertTrue(oldToken.getRevoked());
    }

    @Test
    @Transactional
    void refreshToken_InvalidToken_ThrowsException() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-token");

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> authService.refreshToken(request));
        assertEquals("刷新令牌无效", exception.getMessage());
    }

    @Test
    @Transactional
    void refreshToken_ExpiredToken_ThrowsException() {
        // Arrange: 创建过期的刷新令牌
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        userRepository.save(user);

        RefreshToken expiredToken = new RefreshToken(
                user.getId(),
                "expired-token",
                LocalDateTime.now().minusMinutes(1) // 过期
        );
        refreshTokenRepository.save(expiredToken);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expired-token");

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> authService.refreshToken(request));
        assertEquals("刷新令牌已过期或已撤销", exception.getMessage());
    }

    @Test
    @Transactional
    void refreshToken_RevokedToken_ThrowsException() {
        // Arrange: 创建已撤销的刷新令牌
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        userRepository.save(user);

        RefreshToken revokedToken = new RefreshToken(
                user.getId(),
                "revoked-token",
                LocalDateTime.now().plusDays(7)
        );
        revokedToken.setRevoked(true);
        refreshTokenRepository.save(revokedToken);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("revoked-token");

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> authService.refreshToken(request));
        assertEquals("刷新令牌已过期或已撤销", exception.getMessage());
    }

    // ==================== logout 方法测试 ====================

    @Test
    @Transactional
    void logout_ValidToken_MarksAsRevoked() {
        // Arrange
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        userRepository.save(user);

        RefreshToken refreshToken = authService.createRefreshToken(user.getId());

        // Act
        authService.logout(refreshToken.getToken());

        // Assert
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken.getToken()).orElse(null);
        assertNotNull(token);
        assertTrue(token.getRevoked());
    }

    @Test
    @Transactional
    void logout_InvalidToken_ThrowsException() {
        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> authService.logout("invalid-token"));
        assertEquals("刷新令牌无效", exception.getMessage());
    }

    // ==================== revokeAllUserTokens 方法测试 ====================

    @Test
    @Transactional
    void revokeAllUserTokens_RevokesAllValidTokens() {
        // Arrange
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        userRepository.save(user);

        RefreshToken token1 = authService.createRefreshToken(user.getId());
        RefreshToken token2 = authService.createRefreshToken(user.getId());

        // Act
        authService.revokeAllUserTokens(user.getId());

        // Assert
        RefreshToken updatedToken1 = refreshTokenRepository.findById(token1.getId()).orElse(null);
        RefreshToken updatedToken2 = refreshTokenRepository.findById(token2.getId()).orElse(null);

        assertNotNull(updatedToken1);
        assertNotNull(updatedToken2);
        assertTrue(updatedToken1.getRevoked());
        assertTrue(updatedToken2.getRevoked());
    }

    // ==================== forgotPassword 方法测试 ====================

    @Test
    @Transactional
    void forgotPassword_ExistingEmail_CreatesVerificationCode() {
        // Arrange
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        userRepository.save(user);

        // Act
        authService.forgotPassword("test@example.com");

        // Assert: 验证码已创建
        VerificationCode code = verificationCodeRepository
                .findFirstByEmailAndTypeOrderByCreatedAtDesc("test@example.com", "PASSWORD_RESET")
                .orElse(null);
        assertNotNull(code);
        assertFalse(code.getUsed());
        assertEquals("PASSWORD_RESET", code.getType());
    }

    @Test
    @Transactional
    void forgotPassword_NonExistingEmail_DoesNotThrowException() {
        // Act & Assert: 为了安全，不应抛出异常（防止邮箱枚举）
        assertDoesNotThrow(() -> authService.forgotPassword("nonexistent@example.com"));
    }

    // ==================== resetPassword 方法测试 ====================

    @Test
    @Transactional
    void resetPassword_ValidCode_ResetsPasswordAndUnlocksAccount() {
        // Arrange
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        user.setIsLocked(true);
        user.setFailedLoginAttempts(5);
        userRepository.save(user);

        VerificationCode code = new VerificationCode(
                "test@example.com",
                "123456",
                "PASSWORD_RESET",
                LocalDateTime.now().plusMinutes(10)
        );
        verificationCodeRepository.save(code);

        // Act
        authService.resetPassword("test@example.com", "123456", "newpassword123");

        // Assert: 密码已更新
        User updatedUser = userRepository.findByUsername("testuser").orElse(null);
        assertNotNull(updatedUser);
        assertTrue(passwordEncoder.matches("newpassword123", updatedUser.getPasswordHash()));

        // Assert: 账户已解锁
        assertFalse(updatedUser.getIsLocked());
        assertEquals(0, updatedUser.getFailedLoginAttempts());

        // Assert: 验证码已使用
        VerificationCode updatedCode = verificationCodeRepository.findByEmailAndCode("test@example.com", "123456").orElse(null);
        assertNotNull(updatedCode);
        assertTrue(updatedCode.getUsed());
    }

    @Test
    @Transactional
    void resetPassword_InvalidCode_ThrowsException() {
        // Arrange
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        userRepository.save(user);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class,
                () -> authService.resetPassword("test@example.com", "invalid", "newpassword123"));
        assertEquals("验证码不存在", exception.getMessage());
    }

    @Test
    @Transactional
    void resetPassword_ExpiredCode_ThrowsException() {
        // Arrange
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        userRepository.save(user);

        VerificationCode expiredCode = new VerificationCode(
                "test@example.com",
                "123456",
                "PASSWORD_RESET",
                LocalDateTime.now().minusMinutes(1) // 过期
        );
        verificationCodeRepository.save(expiredCode);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class,
                () -> authService.resetPassword("test@example.com", "123456", "newpassword123"));
        assertEquals("验证码已过期", exception.getMessage());
    }

    @Test
    @Transactional
    void resetPassword_WrongType_ThrowsException() {
        // Arrange
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        userRepository.save(user);

        VerificationCode wrongTypeCode = new VerificationCode(
                "test@example.com",
                "123456",
                "REGISTER", // 错误的类型
                LocalDateTime.now().plusMinutes(10)
        );
        verificationCodeRepository.save(wrongTypeCode);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class,
                () -> authService.resetPassword("test@example.com", "123456", "newpassword123"));
        assertEquals("验证码类型错误", exception.getMessage());
    }

    @Test
    @Transactional
    void resetPassword_AfterReset_AllRefreshTokensRevoked() {
        // Arrange
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        userRepository.save(user);

        RefreshToken token1 = authService.createRefreshToken(user.getId());
        RefreshToken token2 = authService.createRefreshToken(user.getId());

        VerificationCode code = new VerificationCode(
                "test@example.com",
                "123456",
                "PASSWORD_RESET",
                LocalDateTime.now().plusMinutes(10)
        );
        verificationCodeRepository.save(code);

        // Act
        authService.resetPassword("test@example.com", "123456", "newpassword123");

        // Assert: 所有刷新令牌被撤销
        RefreshToken updatedToken1 = refreshTokenRepository.findById(token1.getId()).orElse(null);
        RefreshToken updatedToken2 = refreshTokenRepository.findById(token2.getId()).orElse(null);

        assertNotNull(updatedToken1);
        assertNotNull(updatedToken2);
        assertTrue(updatedToken1.getRevoked());
        assertTrue(updatedToken2.getRevoked());
    }
}
