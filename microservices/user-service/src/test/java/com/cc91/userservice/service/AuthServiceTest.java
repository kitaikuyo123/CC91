package com.cc91.userservice.service;

import com.cc91.userservice.dto.LoginRequest;
import com.cc91.userservice.dto.LoginResponse;
import com.cc91.userservice.dto.RefreshTokenRequest;
import com.cc91.userservice.dto.RefreshTokenResponse;
import com.cc91.userservice.dto.RegisterRequest;
import com.cc91.userservice.dto.RegisterResponse;
import com.cc91.userservice.dto.VerifyEmailRequest;
import com.cc91.userservice.entity.RefreshToken;
import com.cc91.userservice.entity.User;
import com.cc91.userservice.entity.VerificationCode;
import com.cc91.userservice.exception.BadRequestException;
import com.cc91.userservice.exception.ResourceNotFoundException;
import com.cc91.userservice.exception.UnauthorizedException;
import com.cc91.userservice.repository.RefreshTokenRepository;
import com.cc91.userservice.repository.UserRepository;
import com.cc91.userservice.repository.VerificationCodeRepository;
import com.cc91.userservice.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService unit tests — covers the full authentication lifecycle.
 * Uses real BCryptPasswordEncoder + real JwtUtil (no mocking of crypto/JWT).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private VerificationCodeRepository verificationCodeRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private ApplicationEventPublisher eventPublisher;

    private static final BCryptPasswordEncoder VERIFY_ENCODER = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "test-jwt-secret-key-for-cc91-unit-tests-do-not-use-in-production-at-least-256-bits-long");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);

        authService = new AuthService(
                userRepository,
                verificationCodeRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtUtil,
                authenticationManager,
                eventPublisher);
        ReflectionTestUtils.setField(authService, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(authService, "lockDurationSeconds", 30);
        ReflectionTestUtils.setField(authService, "refreshExpiration", 604800000L);
    }

    private RegisterRequest registerRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setUsername("alice");
        r.setEmail("alice@example.com");
        r.setPassword("secret123");
        return r;
    }

    // ============================================================
    // register()
    // ============================================================
    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("should throw BadRequestException when username already exists")
        void shouldThrowWhenUsernameExists() {
            when(userRepository.existsByUsername("alice")).thenReturn(true);
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.register(registerRequest()));
            assertEquals(AuthService.USERNAME_ALREADY_USED, ex.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BadRequestException when email already exists")
        void shouldThrowWhenEmailExists() {
            when(userRepository.existsByUsername("alice")).thenReturn(false);
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.register(registerRequest()));
            assertEquals(AuthService.EMAIL_ALREADY_REGISTERED, ex.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should create locked user + 6-digit code + emit EmailEvent on success")
        void shouldCreateLockedUserAndEmitEvent() {
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);

            RegisterResponse response = authService.register(registerRequest());

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertTrue(saved.getIsLocked(), "newly registered user must be locked until email verified");
            assertNull(saved.getLockUntil());
            assertEquals("alice", saved.getUsername());
            assertEquals("alice@example.com", saved.getEmail());
            assertNotEquals("secret123", saved.getPasswordHash(), "password must be hashed");
            assertTrue(VERIFY_ENCODER.matches("secret123", saved.getPasswordHash()));

            ArgumentCaptor<VerificationCode> codeCaptor =
                    ArgumentCaptor.forClass(VerificationCode.class);
            verify(verificationCodeRepository).save(codeCaptor.capture());
            VerificationCode code = codeCaptor.getValue();
            assertEquals("REGISTER", code.getType());
            assertEquals(6, code.getCode().length(), "verification code must be 6 digits");
            assertTrue(code.getCode().matches("\\d{6}"));
            assertFalse(code.getUsed());

            verify(eventPublisher).publishEvent(any(EmailEvent.class));
            assertEquals(AuthService.VERIFICATION_CODE_SENT, response.getMessage());
            assertEquals(600, response.getExpiresIn());
        }
    }

    // ============================================================
    // verifyEmail()
    // ============================================================
    @Nested
    @DisplayName("verifyEmail()")
    class VerifyEmail {

        @Test
        @DisplayName("should throw BadRequest when code not found")
        void shouldThrowWhenCodeNotFound() {
            when(verificationCodeRepository.findByEmailAndCodeAndType(any(), any(), eq("REGISTER")))
                    .thenReturn(Optional.empty());
            VerifyEmailRequest req = new VerifyEmailRequest();
            req.setEmail("a@b.com");
            req.setCode("123456");
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.verifyEmail(req));
            assertEquals(AuthService.VERIFICATION_CODE_NOT_FOUND, ex.getMessage());
        }

        @Test
        @DisplayName("should throw BadRequest when code already used")
        void shouldThrowWhenCodeUsed() {
            VerificationCode code = new VerificationCode("a@b.com", "123456", "REGISTER",
                    LocalDateTime.now().plusMinutes(5));
            code.setUsed(true);
            when(verificationCodeRepository.findByEmailAndCodeAndType(any(), any(), eq("REGISTER")))
                    .thenReturn(Optional.of(code));
            VerifyEmailRequest req = new VerifyEmailRequest();
            req.setEmail("a@b.com");
            req.setCode("123456");
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.verifyEmail(req));
            assertEquals(AuthService.VERIFICATION_CODE_USED, ex.getMessage());
        }

        @Test
        @DisplayName("should throw BadRequest when code expired")
        void shouldThrowWhenCodeExpired() {
            VerificationCode code = new VerificationCode("a@b.com", "123456", "REGISTER",
                    LocalDateTime.now().minusMinutes(5));
            when(verificationCodeRepository.findByEmailAndCodeAndType(any(), any(), eq("REGISTER")))
                    .thenReturn(Optional.of(code));
            VerifyEmailRequest req = new VerifyEmailRequest();
            req.setEmail("a@b.com");
            req.setCode("123456");
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.verifyEmail(req));
            assertEquals(AuthService.VERIFICATION_CODE_EXPIRED, ex.getMessage());
        }

        @Test
        @DisplayName("should unlock user + mark code used on success")
        void shouldUnlockUserOnSuccess() {
            VerificationCode code = new VerificationCode("a@b.com", "123456", "REGISTER",
                    LocalDateTime.now().plusMinutes(5));
            when(verificationCodeRepository.findByEmailAndCodeAndType("a@b.com", "123456", "REGISTER"))
                    .thenReturn(Optional.of(code));

            User user = new User("alice", "a@b.com", "hash");
            user.setIsLocked(true);
            user.setFailedLoginAttempts(3);
            when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

            VerifyEmailRequest req = new VerifyEmailRequest();
            req.setEmail("a@b.com");
            req.setCode("123456");
            authService.verifyEmail(req);

            assertTrue(code.getUsed(), "verification code must be marked used");
            assertFalse(user.getIsLocked(), "user must be unlocked after verification");
            assertNull(user.getLockUntil());
            assertEquals(0, user.getFailedLoginAttempts());
            verify(userRepository).save(user);
        }
    }

    // ============================================================
    // login()
    // ============================================================
    @Nested
    @DisplayName("login()")
    class Login {

        private LoginRequest loginRequest() {
            LoginRequest r = new LoginRequest();
            r.setUsername("alice");
            r.setPassword("secret123");
            return r;
        }

        @Test
        @DisplayName("should throw Unauthorized when user does not exist")
        void shouldThrowWhenUserMissing() {
            when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> authService.login(loginRequest()));
            assertEquals(AuthService.BAD_CREDENTIALS, ex.getMessage());
            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        @DisplayName("should throw Forbidden-like message when account is banned (isLocked=true, lockUntil=null)")
        void shouldThrowWhenAccountBanned() {
            User user = new User("alice", "a@b.com", "hash");
            user.setIsLocked(true);
            user.setLockUntil(null);
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> authService.login(loginRequest()));
            assertEquals(AuthService.ACCOUNT_BANNED, ex.getMessage());
            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        @DisplayName("should throw Unauthorized with lock prefix when account is temporarily locked")
        void shouldThrowWhenAccountTemporarilyLocked() {
            User user = new User("alice", "a@b.com", "hash");
            user.setIsLocked(true);
            user.setLockUntil(LocalDateTime.now().plusSeconds(30));
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> authService.login(loginRequest()));
            assertTrue(ex.getMessage().startsWith(AuthService.ACCOUNT_LOCKED_PREFIX));
        }

        @Test
        @DisplayName("should auto-unlock when lockUntil has passed, then proceed to authenticate")
        void shouldAutoUnlockWhenLockExpired() {
            User user = new User("alice", "a@b.com", VERIFY_ENCODER.encode("secret123"));
            user.setIsLocked(true);
            user.setLockUntil(LocalDateTime.now().minusSeconds(1));
            user.setFailedLoginAttempts(5);
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LoginResponse resp = authService.login(loginRequest());
            assertNotNull(resp.getAccessToken());
            assertNotNull(resp.getRefreshToken());
            assertEquals("alice", resp.getUsername());
            assertFalse(user.getIsLocked(), "auto-unlock should clear isLocked");
        }

        @Test
        @DisplayName("should increment failed attempts but NOT lock when below threshold")
        void shouldIncrementFailedAttemptsBelowThreshold() {
            User user = new User("alice", "a@b.com", VERIFY_ENCODER.encode("secret123"));
            user.setFailedLoginAttempts(2);
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            doThrow(new BadCredentialsException("bad"))
                    .when(authenticationManager)
                    .authenticate(any(UsernamePasswordAuthenticationToken.class));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> authService.login(loginRequest()));
            assertEquals(AuthService.BAD_CREDENTIALS, ex.getMessage());
            assertEquals(3, user.getFailedLoginAttempts());
            assertFalse(user.getIsLocked());
        }

        @Test
        @DisplayName("should lock account after 5 consecutive failures")
        void shouldLockAfterFiveFailures() {
            User user = new User("alice", "a@b.com", VERIFY_ENCODER.encode("secret123"));
            user.setFailedLoginAttempts(4);
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            doThrow(new BadCredentialsException("bad"))
                    .when(authenticationManager)
                    .authenticate(any(UsernamePasswordAuthenticationToken.class));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> authService.login(loginRequest()));
            assertTrue(ex.getMessage().startsWith(AuthService.ACCOUNT_LOCKED_PREFIX));
            assertEquals(5, user.getFailedLoginAttempts());
            assertTrue(user.getIsLocked());
            assertNotNull(user.getLockUntil());
        }

        @Test
        @DisplayName("should reset failed counter + issue tokens on successful login")
        void shouldResetAndIssueTokensOnSuccess() {
            User user = new User("alice", "a@b.com", VERIFY_ENCODER.encode("secret123"));
            user.setFailedLoginAttempts(3);
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(authenticationManager.authenticate(any())).thenReturn(null);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LoginResponse resp = authService.login(loginRequest());

            assertEquals(0, user.getFailedLoginAttempts());
            assertFalse(user.getIsLocked());
            assertNotNull(resp.getAccessToken());
            assertNotNull(resp.getRefreshToken());
            assertEquals("alice", resp.getUsername());
            assertEquals("USER", resp.getRole());
        }
    }

    // ============================================================
    // refreshToken()
    // ============================================================
    @Nested
    @DisplayName("refreshToken()")
    class Refresh {

        @Test
        @DisplayName("should throw Unauthorized when token not found")
        void shouldThrowWhenTokenMissing() {
            when(refreshTokenRepository.findByToken(any())).thenReturn(Optional.empty());
            RefreshTokenRequest req = new RefreshTokenRequest("nonexistent");
            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> authService.refreshToken(req));
            assertEquals(AuthService.REFRESH_TOKEN_INVALID, ex.getMessage());
        }

        @Test
        @DisplayName("should throw Unauthorized when token is revoked")
        void shouldThrowWhenTokenRevoked() {
            RefreshToken token = new RefreshToken(1L, "tok",
                    LocalDateTime.now().plusDays(1));
            token.setRevoked(true);
            when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(token));

            RefreshTokenRequest req = new RefreshTokenRequest("tok");
            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> authService.refreshToken(req));
            assertEquals(AuthService.REFRESH_TOKEN_EXPIRED_OR_REVOKED, ex.getMessage());
        }

        @Test
        @DisplayName("should throw Unauthorized when token is expired")
        void shouldThrowWhenTokenExpired() {
            RefreshToken token = new RefreshToken(1L, "tok",
                    LocalDateTime.now().minusDays(1));
            when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(token));

            RefreshTokenRequest req = new RefreshTokenRequest("tok");
            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> authService.refreshToken(req));
            assertEquals(AuthService.REFRESH_TOKEN_EXPIRED_OR_REVOKED, ex.getMessage());
        }

        @Test
        @DisplayName("should throw Unauthorized when user is locked")
        void shouldThrowWhenUserLocked() {
            RefreshToken token = new RefreshToken(1L, "tok",
                    LocalDateTime.now().plusDays(1));
            when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(token));
            User user = new User("alice", "a@b.com", "hash");
            user.setIsLocked(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            RefreshTokenRequest req = new RefreshTokenRequest("tok");
            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> authService.refreshToken(req));
            assertEquals(AuthService.ACCOUNT_UNAVAILABLE, ex.getMessage());
        }

        @Test
        @DisplayName("should rotate: revoke old token + issue new token pair")
        void shouldRotateTokens() {
            RefreshToken oldToken = new RefreshToken(1L, "old-tok",
                    LocalDateTime.now().plusDays(1));
            when(refreshTokenRepository.findByToken("old-tok")).thenReturn(Optional.of(oldToken));
            User user = new User("alice", "a@b.com", "hash");
            user.setId(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            // save() returns the new token
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RefreshTokenResponse resp =
                    authService.refreshToken(new RefreshTokenRequest("old-tok"));

            assertTrue(oldToken.getRevoked(), "old token must be revoked after refresh");
            verify(refreshTokenRepository).save(oldToken);
            verify(refreshTokenRepository, times(2)).save(any()); // old + new
            assertNotNull(resp.getAccessToken());
            assertNotNull(resp.getRefreshToken());
            assertNotEquals("old-tok", resp.getRefreshToken());
        }
    }

    // ============================================================
    // logout()
    // ============================================================
    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("should revoke token on logout")
        void shouldRevokeToken() {
            RefreshToken token = new RefreshToken(1L, "tok",
                    LocalDateTime.now().plusDays(1));
            when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(token));

            authService.logout("tok");

            assertTrue(token.getRevoked());
            verify(refreshTokenRepository).save(token);
        }

        @Test
        @DisplayName("should throw Unauthorized when token does not exist")
        void shouldThrowWhenTokenMissing() {
            when(refreshTokenRepository.findByToken(any())).thenReturn(Optional.empty());
            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> authService.logout("ghost"));
            assertEquals(AuthService.REFRESH_TOKEN_INVALID, ex.getMessage());
        }
    }

    // ============================================================
    // forgotPassword()
    // ============================================================
    @Nested
    @DisplayName("forgotPassword()")
    class ForgotPassword {

        @Test
        @DisplayName("should silently return when email not registered (no account enumeration)")
        void shouldReturnSilentlyWhenEmailUnknown() {
            when(userRepository.existsByEmail("ghost@example.com")).thenReturn(false);
            // must not throw, must not emit event
            authService.forgotPassword("ghost@example.com");
            verifyNoInteractions(eventPublisher);
            verify(verificationCodeRepository, never()).save(any());
        }

        @Test
        @DisplayName("should generate password reset code + emit event when email exists")
        void shouldGenerateResetCode() {
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);
            authService.forgotPassword("alice@example.com");
            ArgumentCaptor<VerificationCode> captor = ArgumentCaptor.forClass(VerificationCode.class);
            verify(verificationCodeRepository).save(captor.capture());
            assertEquals("PASSWORD_RESET", captor.getValue().getType());
            assertEquals(6, captor.getValue().getCode().length());
            verify(eventPublisher).publishEvent(any(EmailEvent.class));
        }
    }

    // ============================================================
    // resetPassword()
    // ============================================================
    @Nested
    @DisplayName("resetPassword()")
    class ResetPassword {

        @Test
        @DisplayName("should throw BadRequest when code not found")
        void shouldThrowWhenCodeMissing() {
            when(verificationCodeRepository.findByEmailAndCode(any(), any()))
                    .thenReturn(Optional.empty());
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.resetPassword("a@b.com", "123456", "newpass"));
            assertEquals(AuthService.VERIFICATION_CODE_NOT_FOUND, ex.getMessage());
        }

        @Test
        @DisplayName("should throw BadRequest when code type is REGISTER (mismatch)")
        void shouldThrowWhenCodeTypeMismatch() {
            VerificationCode code = new VerificationCode("a@b.com", "123456", "REGISTER",
                    LocalDateTime.now().plusMinutes(5));
            when(verificationCodeRepository.findByEmailAndCode("a@b.com", "123456"))
                    .thenReturn(Optional.of(code));
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.resetPassword("a@b.com", "123456", "newpass"));
            assertEquals(AuthService.VERIFICATION_CODE_TYPE_INVALID, ex.getMessage());
        }

        @Test
        @DisplayName("should update password (BCrypt) + revoke all refresh tokens")
        void shouldResetPasswordAndRevokeTokens() {
            VerificationCode code = new VerificationCode("a@b.com", "123456", "PASSWORD_RESET",
                    LocalDateTime.now().plusMinutes(5));
            when(verificationCodeRepository.findByEmailAndCode("a@b.com", "123456"))
                    .thenReturn(Optional.of(code));

            User user = new User("alice", "a@b.com", "old-hash");
            user.setId(7L);
            user.setIsLocked(true);
            user.setFailedLoginAttempts(2);
            when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

            RefreshToken t1 = new RefreshToken(7L, "t1", LocalDateTime.now().plusDays(1));
            RefreshToken t2 = new RefreshToken(7L, "t2", LocalDateTime.now().plusDays(1));
            when(refreshTokenRepository.findByUserIdAndRevokedFalse(7L))
                    .thenReturn(List.of(t1, t2));

            authService.resetPassword("a@b.com", "123456", "brand-new-password");

            assertTrue(VERIFY_ENCODER.matches("brand-new-password", user.getPasswordHash()));
            assertFalse(user.getIsLocked());
            assertEquals(0, user.getFailedLoginAttempts());
            assertTrue(code.getUsed());
            assertTrue(t1.getRevoked());
            assertTrue(t2.getRevoked());
            verify(refreshTokenRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("should throw ResourceNotFound when user no longer exists during reset")
        void shouldThrowWhenUserMissing() {
            VerificationCode code = new VerificationCode("a@b.com", "123456", "PASSWORD_RESET",
                    LocalDateTime.now().plusMinutes(5));
            when(verificationCodeRepository.findByEmailAndCode("a@b.com", "123456"))
                    .thenReturn(Optional.of(code));
            when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> authService.resetPassword("a@b.com", "123456", "newpass"));
        }
    }

    // ============================================================
    // createRefreshToken()
    // ============================================================
    @Test
    @DisplayName("createRefreshToken should persist a token with future expiry")
    void shouldPersistNewRefreshToken() {
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        RefreshToken token = authService.createRefreshToken(42L);
        assertEquals(42L, token.getUserId());
        assertNotNull(token.getToken());
        assertTrue(token.getExpiresAt().isAfter(LocalDateTime.now()));
        assertFalse(token.getRevoked());
    }

    // ============================================================
    // revokeAllUserTokens()
    // ============================================================
    @Test
    @DisplayName("revokeAllUserTokens should revoke each active token in batch")
    void revokeAllUserTokensRevokesAll() {
        RefreshToken a = new RefreshToken(1L, "a", LocalDateTime.now().plusDays(1));
        RefreshToken b = new RefreshToken(1L, "b", LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByUserIdAndRevokedFalse(1L)).thenReturn(List.of(a, b));

        authService.revokeAllUserTokens(1L);

        assertTrue(a.getRevoked());
        assertTrue(b.getRevoked());
        verify(refreshTokenRepository).saveAll(anyList());
    }
}
