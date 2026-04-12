package com.cc91.service;

import com.cc91.dto.*;
import com.cc91.entity.User;
import com.cc91.entity.VerificationCode;
import com.cc91.repository.UserRepository;
import com.cc91.repository.VerificationCodeRepository;
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
import java.util.Random;

/**
 * 认证服务
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Value("${account.lock.max-attempts:5}")
    private int maxFailedAttempts;

    @Value("${account.lock.duration-minutes:30}")
    private int lockDurationMinutes;

    public AuthService(
            UserRepository userRepository,
            VerificationCodeRepository verificationCodeRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.verificationCodeRepository = verificationCodeRepository;
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
            String token = jwtUtil.generateToken(user.getUsername());

            logger.info("用户登录成功: {}", user.getUsername());
            return new LoginResponse(token, jwtUtil.getExpiration());

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
}
