package com.cc91.service;

import com.cc91.dto.ChangePasswordRequest;
import com.cc91.dto.UpdateUserProfileRequest;
import com.cc91.dto.UserProfileDTO;
import com.cc91.entity.User;
import com.cc91.entity.UserProfile;
import com.cc91.exception.BadRequestException;
import com.cc91.repository.UserRepository;
import com.cc91.repository.UserProfileRepository;
import jakarta.persistence.EntityManager;
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
 * UserService 业务逻辑测试
 * 测试用户资料查询和更新的业务路径
 */
@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        userProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== getUserProfile 方法测试 ====================

    @Test
    @Transactional
    void getUserProfile_UserExists_ReturnsProfile() {
        // Arrange: 创建一个用户
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        // Act: 获取用户资料
        UserProfileDTO profile = userService.getUserProfile("testuser");

        // Assert: 验证返回的资料
        assertNotNull(profile);
        assertEquals("testuser", profile.getUsername());
        assertEquals("test@example.com", profile.getEmail());
        assertNull(profile.getAvatarUrl());
        assertNull(profile.getBio());
        assertNull(profile.getLocation());
        assertNull(profile.getWebsite());
        assertNotNull(profile.getCreatedAt());
    }

    @Test
    @Transactional
    void getUserProfile_UserExistsWithProfile_ReturnsCompleteProfile() {
        // Arrange: 创建一个用户和资料
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);
        entityManager.refresh(user);

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setAvatarUrl("https://example.com/avatar.jpg");
        profile.setBio("这是我的个人签名");
        profile.setLocation("北京");
        profile.setWebsite("https://example.com");
        userProfileRepository.saveAndFlush(profile);

        // Act: 获取用户资料
        UserProfileDTO result = userService.getUserProfile("testuser");

        // Assert: 验证返回的完整资料
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("https://example.com/avatar.jpg", result.getAvatarUrl());
        assertEquals("这是我的个人签名", result.getBio());
        assertEquals("北京", result.getLocation());
        assertEquals("https://example.com", result.getWebsite());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void getUserProfile_UserNotExists_ThrowsException() {
        // Act & Assert: 查询不存在的用户
        Exception exception = assertThrows(RuntimeException.class, () -> userService.getUserProfile("nonexistent"));
        assertEquals("用户不存在", exception.getMessage());
    }

    // ==================== getMyProfile 方法测试 ====================

    @Test
    @Transactional
    void getMyProfile_UserExists_ReturnsProfile() {
        // Arrange: 创建一个用户
        User user = new User("myuser", "my@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        // Act: 获取自己的资料
        UserProfileDTO profile = userService.getMyProfile("myuser");

        // Assert: 验证返回的资料
        assertNotNull(profile);
        assertEquals("myuser", profile.getUsername());
        assertEquals("my@example.com", profile.getEmail());
    }

    // ==================== updateProfile 方法测试 ====================

    @Test
    @Transactional
    void updateProfile_UserExists_CreatesNewProfile() {
        // Arrange: 创建一个用户（没有资料）
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setAvatarUrl("https://example.com/avatar.jpg");
        request.setBio("我的签名");
        request.setLocation("上海");
        request.setWebsite("https://mysite.com");

        // Act: 更新资料
        UserProfileDTO result = userService.updateProfile("testuser", request);

        // Assert: 验证返回的资料
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("https://example.com/avatar.jpg", result.getAvatarUrl());
        assertEquals("我的签名", result.getBio());
        assertEquals("上海", result.getLocation());
        assertEquals("https://mysite.com", result.getWebsite());

        // Assert: 验证数据库中已保存
        UserProfile savedProfile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        assertNotNull(savedProfile);
        assertEquals("https://example.com/avatar.jpg", savedProfile.getAvatarUrl());
    }

    @Test
    @Transactional
    void updateProfile_ProfileExists_UpdatesExistingProfile() {
        // Arrange: 创建一个用户和已有资料
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);
        entityManager.refresh(user);

        UserProfile existingProfile = new UserProfile();
        existingProfile.setUser(user);
        existingProfile.setAvatarUrl("https://old.com/avatar.jpg");
        existingProfile.setBio("旧签名");
        userProfileRepository.saveAndFlush(existingProfile);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setAvatarUrl("https://new.com/avatar.jpg");
        request.setBio("新签名");

        // Act: 更新资料
        UserProfileDTO result = userService.updateProfile("testuser", request);

        // Assert: 验证返回的资料已更新
        assertEquals("https://new.com/avatar.jpg", result.getAvatarUrl());
        assertEquals("新签名", result.getBio());
    }

    @Test
    void updateProfile_UserNotExists_ThrowsException() {
        // Arrange: 准备更新请求
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setBio("测试签名");

        // Act & Assert: 更新不存在用户的资料
        Exception exception = assertThrows(RuntimeException.class, () -> userService.updateProfile("nonexistent", request));
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    @Transactional
    void updateProfile_PartialFields_UpdatesOnlyProvidedFields() {
        // Arrange: 创建一个用户
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        // 先设置一些初始值
        UpdateUserProfileRequest initialRequest = new UpdateUserProfileRequest();
        initialRequest.setAvatarUrl("https://old.com/avatar.jpg");
        initialRequest.setBio("旧签名");
        initialRequest.setLocation("旧地点");
        initialRequest.setWebsite("https://old.com");
        userService.updateProfile("testuser", initialRequest);

        // Act: 只更新部分字段
        UpdateUserProfileRequest partialRequest = new UpdateUserProfileRequest();
        partialRequest.setBio("新签名");
        // 其他字段为 null，不应被更新

        UserProfileDTO result = userService.updateProfile("testuser", partialRequest);

        // Assert: 验证只有 bio 被更新，其他保持原值
        assertEquals("https://old.com/avatar.jpg", result.getAvatarUrl());
        assertEquals("新签名", result.getBio());
        assertEquals("旧地点", result.getLocation());
        assertEquals("https://old.com", result.getWebsite());
    }

    @Test
    @Transactional
    void updateProfile_EmptyFields_AllowsEmptyStrings() {
        // Arrange: 创建一个用户
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(user);

        // 先设置一些值
        UpdateUserProfileRequest initialRequest = new UpdateUserProfileRequest();
        initialRequest.setBio("旧签名");
        userService.updateProfile("testuser", initialRequest);

        // Act: 更新为空字符串（清空字段）
        UpdateUserProfileRequest clearRequest = new UpdateUserProfileRequest();
        clearRequest.setBio("");

        UserProfileDTO result = userService.updateProfile("testuser", clearRequest);

        // Assert: 验证空字符串被保存
        assertEquals("", result.getBio());

        // 验证数据库中也是空字符串
        UserProfile savedProfile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        assertNotNull(savedProfile);
        assertEquals("", savedProfile.getBio());
    }

    // ==================== changePassword 方法测试 ====================

    @Test
    @Transactional
    void changePassword_CorrectOldPassword_Success() {
        // Arrange: 创建用户，记录旧密码哈希
        String oldPassword = "oldPassword123";
        User user = new User("testuser", "test@example.com", passwordEncoder.encode(oldPassword));
        user = userRepository.saveAndFlush(user);
        String oldHash = user.getPasswordHash();

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword(oldPassword);
        request.setNewPassword("newPassword456");

        // Act
        userService.changePassword("testuser", request);

        // Assert: 密码哈希已变更
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertNotEquals(oldHash, updatedUser.getPasswordHash());
        assertTrue(passwordEncoder.matches("newPassword456", updatedUser.getPasswordHash()));
    }

    @Test
    @Transactional
    void changePassword_WrongOldPassword_ThrowsBadRequestException() {
        // Arrange
        User user = new User("testuser", "test@example.com", passwordEncoder.encode("correctPassword"));
        userRepository.saveAndFlush(user);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrongPassword");
        request.setNewPassword("newPassword123");

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> userService.changePassword("testuser", request));
        assertEquals("旧密码不正确", ex.getMessage());
    }

    @Test
    @Transactional
    void changePassword_UserNotExists_ThrowsRuntimeException() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("anyPassword");
        request.setNewPassword("newPassword123");

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.changePassword("nonexistent", request));
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    @Transactional
    void changePassword_ShortNewPassword_SuccessAtServiceLevel() {
        // 注：新密码长度校验 (@Size(min=6)) 通过 @Valid 在 controller 层触发，
        // service 层不做此校验，短密码在 service 层能成功修改。
        // Arrange
        String oldPassword = "oldPassword123";
        User user = new User("testuser", "test@example.com", passwordEncoder.encode(oldPassword));
        userRepository.saveAndFlush(user);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword(oldPassword);
        request.setNewPassword("ab");  // 仅 2 位，太短

        // Act & Assert: service 层不校验长度，不会抛异常
        assertDoesNotThrow(() -> userService.changePassword("testuser", request));

        // 验证密码确实被修改了
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("ab", updatedUser.getPasswordHash()));
    }
}
