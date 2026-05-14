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
import com.cc91.exception.BadRequestException;
import com.cc91.exception.ResourceNotFoundException;
import com.cc91.exception.UnauthorizedException;
import com.cc91.repository.RefreshTokenRepository;
import com.cc91.repository.UserRepository;
import com.cc91.repository.VerificationCodeRepository;
import com.cc91.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    public static final String USERNAME_ALREADY_USED = "Username already exists";
    public static final String EMAIL_ALREADY_REGISTERED = "Email already registered";
    public static final String VERIFICATION_CODE_SENT = "Verification code sent";
    public static final String VERIFICATION_CODE_NOT_FOUND = "Verification code not found";
    public static final String VERIFICATION_CODE_USED = "Verification code already used";
    public static final String VERIFICATION_CODE_EXPIRED = "Verification code expired";
    public static final String VERIFICATION_CODE_TYPE_INVALID = "Verification code type invalid";
    public static final String USER_NOT_FOUND = "User not found";
    public static final String BAD_CREDENTIALS = "Invalid username or password";
    public static final String EMAIL_NOT_VERIFIED = "Please verify your email first";
    public static final String ACCOUNT_LOCKED_PREFIX = "Account locked, try again in ";
    public static final String ACCOUNT_UNAVAILABLE = "Account unavailable, please log in again";
    public static final String REFRESH_TOKEN_INVALID = "Refresh token invalid";
    public static final String REFRESH_TOKEN_EXPIRED_OR_REVOKED = "Refresh token expired or revoked";

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String REGISTER_CODE_TYPE = "REGISTER";
    private static final String PASSWORD_RESET_CODE_TYPE = "PASSWORD_RESET";
    private static final int VERIFICATION_CODE_EXPIRES_IN_SECONDS = 600;

    private final UserRepository userRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${account.lock.max-attempts:5}")
    private int maxFailedAttempts;

    @Value("${account.lock.duration-minutes:30}")
    private int lockDurationMinutes;

    @Value("${jwt.refresh-expiration:604800000}")
    private Long refreshExpiration;

    public AuthService(
            UserRepository userRepository,
            VerificationCodeRepository verificationCodeRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException(USERNAME_ALREADY_USED);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException(EMAIL_ALREADY_REGISTERED);
        }

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );
        user.setIsLocked(true);
        user.setLockUntil(null);
        userRepository.save(user);

        String code = generateVerificationCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(VERIFICATION_CODE_EXPIRES_IN_SECONDS);
        verificationCodeRepository.save(new VerificationCode(request.getEmail(), code, REGISTER_CODE_TYPE, expiresAt));

        logger.info("Register verification code for {}: {}, expiresAt={}", request.getEmail(), code, expiresAt);
        return new RegisterResponse(VERIFICATION_CODE_SENT, VERIFICATION_CODE_EXPIRES_IN_SECONDS);
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        VerificationCode verificationCode = verificationCodeRepository
                .findByEmailAndCodeAndType(request.getEmail(), request.getCode(), REGISTER_CODE_TYPE)
                .orElseThrow(() -> new BadRequestException(VERIFICATION_CODE_NOT_FOUND));

        validateVerificationCode(verificationCode);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        verificationCode.setUsed(true);
        verificationCodeRepository.save(verificationCode);

        user.setIsLocked(false);
        user.setLockUntil(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        logger.info("Email verified and account enabled: {}", request.getEmail());
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException(BAD_CREDENTIALS));

        ensureUserCanAttemptLogin(user);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            recordFailedLogin(user);
            throw new UnauthorizedException(BAD_CREDENTIALS);
        }

        resetFailedLoginState(user);

        String accessToken = jwtUtil.generateToken(user.getUsername());
        RefreshToken refreshToken = createRefreshToken(user.getId());

        logger.info("User logged in: {}", user.getUsername());
        return new LoginResponse(
                accessToken,
                refreshToken.getToken(),
                jwtUtil.getExpiration(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }

    private void ensureUserCanAttemptLogin(User user) {
        if (!Boolean.TRUE.equals(user.getIsLocked())) {
            return;
        }

        if (user.getLockUntil() == null) {
            throw new UnauthorizedException(EMAIL_NOT_VERIFIED);
        }

        if (LocalDateTime.now().isBefore(user.getLockUntil())) {
            long remainingMinutes = Duration.between(LocalDateTime.now(), user.getLockUntil()).toMinutes();
            throw new UnauthorizedException(ACCOUNT_LOCKED_PREFIX + Math.max(1, remainingMinutes) + " minute(s)");
        }

        user.setIsLocked(false);
        user.setLockUntil(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
    }

    private void recordFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() == null ? 1 : user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= maxFailedAttempts) {
            user.setIsLocked(true);
            user.setLockUntil(LocalDateTime.now().plusMinutes(lockDurationMinutes));
            logger.warn("Account locked after failed login attempts: username={}, attempts={}", user.getUsername(), attempts);
        }

        userRepository.save(user);
        logger.warn("Login failed: username={}, attempts={}", user.getUsername(), attempts);
    }

    private void resetFailedLoginState(User user) {
        if (user.getFailedLoginAttempts() == null || user.getFailedLoginAttempts() == 0) {
            return;
        }

        user.setFailedLoginAttempts(0);
        user.setIsLocked(false);
        user.setLockUntil(null);
        userRepository.save(user);
    }

    private String generateVerificationCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        RefreshToken refreshToken = new RefreshToken(
                userId,
                UUID.randomUUID().toString(),
                LocalDateTime.now().plusSeconds(refreshExpiration / 1000)
        );
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException(REFRESH_TOKEN_INVALID));

        if (!refreshToken.isValid()) {
            throw new UnauthorizedException(REFRESH_TOKEN_EXPIRED_OR_REVOKED);
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            throw new UnauthorizedException(ACCOUNT_UNAVAILABLE);
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        String newAccessToken = jwtUtil.generateToken(user.getUsername());
        RefreshToken newRefreshToken = createRefreshToken(user.getId());

        logger.info("Token refreshed: {}", user.getUsername());
        return new RefreshTokenResponse(newAccessToken, newRefreshToken.getToken(), jwtUtil.getExpiration());
    }

    @Transactional
    public void logout(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException(REFRESH_TOKEN_INVALID));

        token.setRevoked(true);
        refreshTokenRepository.save(token);

        logger.info("User logged out: userId={}", token.getUserId());
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        tokens.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(tokens);
        logger.info("Revoked all refresh tokens for userId={}", userId);
    }

    @Transactional
    public void forgotPassword(String email) {
        if (!userRepository.existsByEmail(email)) {
            logger.warn("Password reset requested for unknown email: {}", email);
            return;
        }

        String code = generateVerificationCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(VERIFICATION_CODE_EXPIRES_IN_SECONDS);
        verificationCodeRepository.save(new VerificationCode(email, code, PASSWORD_RESET_CODE_TYPE, expiresAt));

        logger.info("Password reset verification code for {}: {}, expiresAt={}", email, code, expiresAt);
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        VerificationCode verificationCode = verificationCodeRepository
                .findByEmailAndCode(email, code)
                .orElseThrow(() -> new BadRequestException(VERIFICATION_CODE_NOT_FOUND));

        validateVerificationCode(verificationCode);
        if (!PASSWORD_RESET_CODE_TYPE.equals(verificationCode.getType())) {
            throw new BadRequestException(VERIFICATION_CODE_TYPE_INVALID);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setIsLocked(false);
        user.setLockUntil(null);
        userRepository.save(user);

        verificationCode.setUsed(true);
        verificationCodeRepository.save(verificationCode);

        revokeAllUserTokens(user.getId());
        logger.info("Password reset completed: {}", email);
    }

    private void validateVerificationCode(VerificationCode verificationCode) {
        if (verificationCode.getUsed()) {
            throw new BadRequestException(VERIFICATION_CODE_USED);
        }
        if (verificationCode.isExpired()) {
            throw new BadRequestException(VERIFICATION_CODE_EXPIRED);
        }
    }
}
