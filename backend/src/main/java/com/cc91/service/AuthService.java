package com.cc91.service;

import com.cc91.dto.*;
import com.cc91.entity.User;
import com.cc91.entity.VerificationCode;
import com.cc91.entity.RefreshToken;
import com.cc91.repository.UserRepository;
import com.cc91.repository.VerificationCodeRepository;
import com.cc91.repository.RefreshTokenRepository;
import com.cc91.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 认证服务
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

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

    /**
     * 用户注册 - 发送验证码
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // 检查用户名是否存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已被使用");
        }

        // 检查邮箱是否存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 生成 6 位数字验证码
        String code = generateVerificationCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        // 保存验证码
        VerificationCode verificationCode = new VerificationCode(
                request.getEmail(),
                code,
                "REGISTER",
                expiresAt
        );
        verificationCodeRepository.save(verificationCode);

        // 打印验证码到日志（开发环境）
        logger.info("====== 验证码 =======");
        logger.info("邮箱: {}", request.getEmail());
        logger.info("验证码: {}", code);
        logger.info("过期时间: {}", expiresAt);
        logger.info("====================");

        return new RegisterResponse("验证码已发送至邮箱", 600);
    }

    /**
     * 验证邮箱验证码并创建用户
     */
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        VerificationCode verificationCode = verificationCodeRepository
                .findByEmailAndCode(request.getEmail(), request.getCode())
                .orElseThrow(() -> new RuntimeException("验证码不存在"));

        // 检查验证码是否已使用
        if (verificationCode.getUsed()) {
            throw new RuntimeException("验证码已使用");
        }

        // 检查验证码是否过期
        if (verificationCode.isExpired()) {
            throw new RuntimeException("验证码已过期");
        }

        // 标记验证码为已使用
        verificationCode.setUsed(true);
        verificationCodeRepository.save(verificationCode);

        logger.info("邮箱验证成功: {}", request.getEmail());
    }

    /**
     * 用户登录
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        // 检查账户是否锁定
        if (user.isAccountLocked()) {
            long remainingMinutes = java.time.Duration.between(
                    LocalDateTime.now(),
                    user.getLockUntil()
            ).toMinutes();
            throw new RuntimeException("账户已锁定，请 " + remainingMinutes + " 分钟后重试");
        }

        try {
            // 认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // 重置失败次数
            if (user.getFailedLoginAttempts() > 0) {
                user.setFailedLoginAttempts(0);
                user.setIsLocked(false);
                user.setLockUntil(null);
                userRepository.save(user);
            }

            // 生成 JWT
            String accessToken = jwtUtil.generateToken(user.getUsername());

            // 生成 Refresh Token
            RefreshToken refreshToken = createRefreshToken(user.getId());

            logger.info("用户登录成功: {}", user.getUsername());
            return new LoginResponse(accessToken, refreshToken.getToken(), jwtUtil.getExpiration());

        } catch (Exception e) {
            // 增加失败次数
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            userRepository.save(user);

            // 检查是否需要锁定
            if (attempts >= maxFailedAttempts) {
                user.setIsLocked(true);
                user.setLockUntil(LocalDateTime.now().plusMinutes(lockDurationMinutes));
                userRepository.save(user);
                logger.warn("账户已锁定: {} (失败次数: {})", user.getUsername(), attempts);
            }

            logger.warn("登录失败: {} (失败次数: {})", user.getUsername(), attempts);
            throw new RuntimeException("用户名或密码错误");
        }
    }

    /**
     * 生成 6 位数字验证码
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 创建刷新令牌
     */
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        RefreshToken refreshToken = new RefreshToken(
                userId,
                UUID.randomUUID().toString(),
                LocalDateTime.now().plusSeconds(refreshExpiration / 1000)
        );
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * 刷新访问令牌（令牌轮换）
     */
    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("刷新令牌无效"));

        // 验证令牌是否有效
        if (!refreshToken.isValid()) {
            throw new RuntimeException("刷新令牌已过期或已撤销");
        }

        // 获取用户信息
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 撤销旧的刷新令牌（令牌轮换）
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // 生成新的访问令牌
        String newAccessToken = jwtUtil.generateToken(user.getUsername());

        // 生成新的刷新令牌
        RefreshToken newRefreshToken = createRefreshToken(user.getId());

        logger.info("令牌刷新成功: {}", user.getUsername());
        return new RefreshTokenResponse(newAccessToken, newRefreshToken.getToken(), jwtUtil.getExpiration());
    }

    /**
     * 撤销刷新令牌（登出）
     */
    @Transactional
    public void logout(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("刷新令牌无效"));

        token.setRevoked(true);
        refreshTokenRepository.save(token);

        logger.info("用户登出: userId={}", token.getUserId());
    }

    /**
     * 撤销用户的所有刷新令牌
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        tokens.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(tokens);
        logger.info("已撤销用户 {} 的所有令牌", userId);
    }

    /**
     * 请求密码重置 - 发送验证码
     */
    @Transactional
    public void forgotPassword(String email) {
        // 检查邮箱是否存在
        if (!userRepository.existsByEmail(email)) {
            // 为了安全，即使用户不存在也返回成功（防止邮箱枚举）
            logger.warn("密码重置请求，邮箱不存在: {}", email);
            return;
        }

        // 生成 6 位数字验证码
        String code = generateVerificationCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        // 保存验证码
        VerificationCode verificationCode = new VerificationCode(
                email,
                code,
                "PASSWORD_RESET",
                expiresAt
        );
        verificationCodeRepository.save(verificationCode);

        // 打印验证码到日志（开发环境）
        logger.info("====== 密码重置验证码 =======");
        logger.info("邮箱: {}", email);
        logger.info("验证码: {}", code);
        logger.info("过期时间: {}", expiresAt);
        logger.info("==========================");
    }

    /**
     * 验证码验证 + 重置密码
     */
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        // 验证验证码
        VerificationCode verificationCode = verificationCodeRepository
                .findByEmailAndCode(email, code)
                .orElseThrow(() -> new RuntimeException("验证码不存在"));

        // 检查验证码是否已使用
        if (verificationCode.getUsed()) {
            throw new RuntimeException("验证码已使用");
        }

        // 检查验证码是否过期
        if (verificationCode.isExpired()) {
            throw new RuntimeException("验证码已过期");
        }

        // 检查验证码类型
        if (!"PASSWORD_RESET".equals(verificationCode.getType())) {
            throw new RuntimeException("验证码类型错误");
        }

        // 获取用户
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(newPassword));

        // 清除账户锁定状态和失败计数
        user.setFailedLoginAttempts(0);
        user.setIsLocked(false);
        user.setLockUntil(null);

        userRepository.save(user);

        // 标记验证码为已使用
        verificationCode.setUsed(true);
        verificationCodeRepository.save(verificationCode);

        // 撤销用户的所有刷新令牌（强制重新登录）
        revokeAllUserTokens(user.getId());

        logger.info("密码重置成功: {}", email);
    }
}
