package com.cc91.service;

import com.cc91.dto.ChangePasswordRequest;
import com.cc91.dto.UpdateUserProfileRequest;
import com.cc91.dto.UserProfileDTO;
import com.cc91.entity.User;
import com.cc91.entity.UserProfile;
import com.cc91.exception.BadRequestException;
import com.cc91.repository.UserRepository;
import com.cc91.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 按用户名查询用户资料（查看他人资料）
     */
    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElse(new UserProfile(user.getId()));

        return new UserProfileDTO(
                user.getUsername(),
                user.getEmail(),
                profile.getAvatarUrl(),
                profile.getBio(),
                profile.getLocation(),
                profile.getWebsite(),
                user.getCreatedAt(),
                user.getRole()
        );
    }

    /**
     * 获取当前用户资料
     */
    @Transactional(readOnly = true)
    public UserProfileDTO getMyProfile(String username) {
        return getUserProfile(username);
    }

    /**
     * 更新用户资料
     */
    @Transactional
    public UserProfileDTO updateProfile(String username, UpdateUserProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserProfile newProfile = new UserProfile();
                    newProfile.setUser(user);
                    return newProfile;
                });

        // 更新字段
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getLocation() != null) {
            profile.setLocation(request.getLocation());
        }
        if (request.getWebsite() != null) {
            profile.setWebsite(request.getWebsite());
        }

        userProfileRepository.save(profile);

        logger.info("用户资料更新成功: {}", username);

        return new UserProfileDTO(
                user.getUsername(),
                user.getEmail(),
                profile.getAvatarUrl(),
                profile.getBio(),
                profile.getLocation(),
                profile.getWebsite(),
                user.getCreatedAt(),
                user.getRole()
        );
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("旧密码不正确");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        logger.info("密码修改成功: {}", username);
    }
}
