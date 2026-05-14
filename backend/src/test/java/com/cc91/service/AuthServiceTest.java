package com.cc91.service;

import com.cc91.dto.LoginRequest;
import com.cc91.dto.LoginResponse;
import com.cc91.dto.RefreshTokenRequest;
import com.cc91.dto.RefreshTokenResponse;
import com.cc91.dto.RegisterRequest;
import com.cc91.dto.RegisterResponse;
import com.cc91.dto.VerifyEmailRequest;
import com.cc91.entity.RefreshToken;
import com.cc91.entity.User;
import com.cc91.entity.VerificationCode;
import com.cc91.repository.RefreshTokenRepository;
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
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-service-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "app.dev-data.enabled=false"
})
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

    @Test
    @Transactional
    void register_Success_CreatesLockedUserAndVerificationCode() {
        RegisterResponse response = authService.register(registerRequest("newuser", "new@example.com", "password123"));

        assertEquals(AuthService.VERIFICATION_CODE_SENT, response.getMessage());
        assertEquals(600, response.getExpiresIn());

        User user = userRepository.findByUsername("newuser").orElseThrow();
        assertTrue(user.getIsLocked());
        assertNull(user.getLockUntil());
        assertTrue(passwordEncoder.matches("password123", user.getPasswordHash()));

        VerificationCode code = verificationCodeRepository
                .findFirstByEmailAndTypeOrderByCreatedAtDesc("new@example.com", "REGISTER")
                .orElseThrow();
        assertFalse(code.getUsed());
        assertTrue(code.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    @Transactional
    void register_DuplicateUsernameOrEmail_ThrowsBadRequest() {
        userRepository.save(new User("existing", "existing@example.com", passwordEncoder.encode("password123")));

        Exception usernameException = assertThrows(RuntimeException.class,
                () -> authService.register(registerRequest("existing", "new@example.com", "password123")));
        assertEquals(AuthService.USERNAME_ALREADY_USED, usernameException.getMessage());

        Exception emailException = assertThrows(RuntimeException.class,
                () -> authService.register(registerRequest("newuser", "existing@example.com", "password123")));
        assertEquals(AuthService.EMAIL_ALREADY_REGISTERED, emailException.getMessage());
    }

    @Test
    @Transactional
    void verifyEmail_ValidCode_UnlocksUser() {
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        user.setIsLocked(true);
        userRepository.save(user);
        verificationCodeRepository.save(new VerificationCode(
                "test@example.com",
                "123456",
                "REGISTER",
                LocalDateTime.now().plusMinutes(10)
        ));

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("test@example.com");
        request.setCode("123456");

        authService.verifyEmail(request);

        User updatedUser = userRepository.findByUsername("testuser").orElseThrow();
        assertFalse(updatedUser.getIsLocked());

        VerificationCode usedCode = verificationCodeRepository.findByEmailAndCode("test@example.com", "123456").orElseThrow();
        assertTrue(usedCode.getUsed());
    }

    @Test
    @Transactional
    void verifyEmail_InvalidUsedOrExpiredCode_ThrowsBadRequest() {
        VerifyEmailRequest missing = new VerifyEmailRequest();
        missing.setEmail("test@example.com");
        missing.setCode("000000");
        assertEquals(AuthService.VERIFICATION_CODE_NOT_FOUND,
                assertThrows(RuntimeException.class, () -> authService.verifyEmail(missing)).getMessage());

        VerificationCode used = new VerificationCode("test@example.com", "111111", "REGISTER", LocalDateTime.now().plusMinutes(10));
        used.setUsed(true);
        verificationCodeRepository.save(used);
        VerifyEmailRequest usedRequest = new VerifyEmailRequest();
        usedRequest.setEmail("test@example.com");
        usedRequest.setCode("111111");
        assertEquals(AuthService.VERIFICATION_CODE_USED,
                assertThrows(RuntimeException.class, () -> authService.verifyEmail(usedRequest)).getMessage());

        verificationCodeRepository.save(new VerificationCode("test@example.com", "222222", "REGISTER", LocalDateTime.now().minusMinutes(1)));
        VerifyEmailRequest expiredRequest = new VerifyEmailRequest();
        expiredRequest.setEmail("test@example.com");
        expiredRequest.setCode("222222");
        assertEquals(AuthService.VERIFICATION_CODE_EXPIRED,
                assertThrows(RuntimeException.class, () -> authService.verifyEmail(expiredRequest)).getMessage());
    }

    @Test
    @Transactional
    void login_Success_ReturnsAccessAndRefreshTokensAndUserInfo() {
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        user.setFailedLoginAttempts(2);
        userRepository.save(user);

        LoginResponse response = authService.login(loginRequest("testuser", "password123"));

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertTrue(response.getExpiresIn() > 0);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("USER", response.getRole());

        User updatedUser = userRepository.findByUsername("testuser").orElseThrow();
        assertEquals(0, updatedUser.getFailedLoginAttempts());
        assertFalse(updatedUser.getIsLocked());
        assertNull(updatedUser.getLockUntil());
    }

    @Test
    @Transactional
    void login_UnverifiedEmail_ThrowsUnauthorizedWithoutNullPointer() {
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        user.setIsLocked(true);
        user.setLockUntil(null);
        userRepository.save(user);

        Exception exception = assertThrows(RuntimeException.class, () -> authService.login(loginRequest("testuser", "password123")));
        assertEquals(AuthService.EMAIL_NOT_VERIFIED, exception.getMessage());
    }

    @Test
    @Transactional
    void login_WrongPassword_IncrementsAttemptsAndLocksAtLimit() {
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        user.setFailedLoginAttempts(4);
        userRepository.save(user);

        Exception exception = assertThrows(RuntimeException.class, () -> authService.login(loginRequest("testuser", "wrong")));
        assertTrue(exception.getMessage().startsWith(AuthService.ACCOUNT_LOCKED_PREFIX));

        User updatedUser = userRepository.findByUsername("testuser").orElseThrow();
        assertEquals(5, updatedUser.getFailedLoginAttempts());
        assertTrue(updatedUser.getIsLocked());
        assertNotNull(updatedUser.getLockUntil());
    }

    @Test
    void login_FiveWrongPasswords_PersistsLockAndBlocksCorrectPassword() {
        userRepository.save(new User("testuser", "test@example.com", passwordEncoder.encode("password123")));

        for (int i = 1; i <= 4; i++) {
            Exception exception = assertThrows(RuntimeException.class,
                    () -> authService.login(loginRequest("testuser", "wrong")));
            assertEquals(AuthService.BAD_CREDENTIALS, exception.getMessage());
        }

        Exception lockException = assertThrows(RuntimeException.class,
                () -> authService.login(loginRequest("testuser", "wrong")));
        assertTrue(lockException.getMessage().startsWith(AuthService.ACCOUNT_LOCKED_PREFIX));

        User lockedUser = userRepository.findByUsername("testuser").orElseThrow();
        assertEquals(5, lockedUser.getFailedLoginAttempts());
        assertTrue(lockedUser.getIsLocked());
        assertNotNull(lockedUser.getLockUntil());
        long remainingSeconds = Duration.between(LocalDateTime.now(), lockedUser.getLockUntil()).toSeconds();
        assertTrue(remainingSeconds > 0 && remainingSeconds <= 30);

        Exception correctPasswordException = assertThrows(RuntimeException.class,
                () -> authService.login(loginRequest("testuser", "password123")));
        assertTrue(correctPasswordException.getMessage().startsWith(AuthService.ACCOUNT_LOCKED_PREFIX));
    }

    @Test
    @Transactional
    void login_ExpiredTimedLock_UnlocksAndAuthenticates() {
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        user.setIsLocked(true);
        user.setLockUntil(LocalDateTime.now().minusMinutes(1));
        user.setFailedLoginAttempts(5);
        userRepository.save(user);

        LoginResponse response = authService.login(loginRequest("testuser", "password123"));

        assertNotNull(response.getAccessToken());
        User updatedUser = userRepository.findByUsername("testuser").orElseThrow();
        assertFalse(updatedUser.getIsLocked());
        assertEquals(0, updatedUser.getFailedLoginAttempts());
        assertNull(updatedUser.getLockUntil());
    }

    @Test
    @Transactional
    void refreshToken_ValidToken_RotatesRefreshToken() {
        User user = userRepository.save(new User("testuser", "test@example.com", passwordEncoder.encode("password123")));
        RefreshToken original = authService.createRefreshToken(user.getId());

        RefreshTokenResponse response = authService.refreshToken(new RefreshTokenRequest(original.getToken()));

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertNotEquals(original.getToken(), response.getRefreshToken());
        assertTrue(refreshTokenRepository.findByToken(original.getToken()).orElseThrow().getRevoked());
    }

    @Test
    @Transactional
    void refreshToken_InvalidExpiredRevokedOrLockedAccount_ThrowsUnauthorized() {
        assertEquals(AuthService.REFRESH_TOKEN_INVALID, assertThrows(RuntimeException.class,
                () -> authService.refreshToken(new RefreshTokenRequest("missing"))).getMessage());

        User user = userRepository.save(new User("testuser", "test@example.com", passwordEncoder.encode("password123")));
        refreshTokenRepository.save(new RefreshToken(user.getId(), "expired", LocalDateTime.now().minusMinutes(1)));
        assertEquals(AuthService.REFRESH_TOKEN_EXPIRED_OR_REVOKED, assertThrows(RuntimeException.class,
                () -> authService.refreshToken(new RefreshTokenRequest("expired"))).getMessage());

        RefreshToken revoked = new RefreshToken(user.getId(), "revoked", LocalDateTime.now().plusDays(7));
        revoked.setRevoked(true);
        refreshTokenRepository.save(revoked);
        assertEquals(AuthService.REFRESH_TOKEN_EXPIRED_OR_REVOKED, assertThrows(RuntimeException.class,
                () -> authService.refreshToken(new RefreshTokenRequest("revoked"))).getMessage());

        RefreshToken lockedToken = refreshTokenRepository.save(new RefreshToken(user.getId(), "locked", LocalDateTime.now().plusDays(7)));
        user.setIsLocked(true);
        user.setLockUntil(null);
        userRepository.save(user);
        assertEquals(AuthService.ACCOUNT_UNAVAILABLE, assertThrows(RuntimeException.class,
                () -> authService.refreshToken(new RefreshTokenRequest(lockedToken.getToken()))).getMessage());
    }

    @Test
    @Transactional
    void logout_ValidToken_RevokesRefreshToken() {
        User user = userRepository.save(new User("testuser", "test@example.com", passwordEncoder.encode("password123")));
        RefreshToken token = authService.createRefreshToken(user.getId());

        authService.logout(token.getToken());

        assertTrue(refreshTokenRepository.findByToken(token.getToken()).orElseThrow().getRevoked());
    }

    @Test
    @Transactional
    void resetPassword_ValidCode_UpdatesPasswordUnlocksAndRevokesSessions() {
        User user = userRepository.save(new User("testuser", "test@example.com", passwordEncoder.encode("password123")));
        user.setIsLocked(true);
        user.setFailedLoginAttempts(5);
        userRepository.save(user);
        RefreshToken token = authService.createRefreshToken(user.getId());
        verificationCodeRepository.save(new VerificationCode(
                "test@example.com",
                "123456",
                "PASSWORD_RESET",
                LocalDateTime.now().plusMinutes(10)
        ));

        authService.resetPassword("test@example.com", "123456", "newpassword123");

        User updatedUser = userRepository.findByUsername("testuser").orElseThrow();
        assertTrue(passwordEncoder.matches("newpassword123", updatedUser.getPasswordHash()));
        assertFalse(updatedUser.getIsLocked());
        assertEquals(0, updatedUser.getFailedLoginAttempts());
        assertTrue(refreshTokenRepository.findByToken(token.getToken()).orElseThrow().getRevoked());
    }

    private RegisterRequest registerRequest(String username, String email, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
}
