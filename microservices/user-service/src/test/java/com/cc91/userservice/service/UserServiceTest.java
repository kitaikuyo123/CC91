package com.cc91.userservice.service;

import com.cc91.userservice.dto.ChangePasswordRequest;
import com.cc91.userservice.dto.UpdateUserProfileRequest;
import com.cc91.userservice.dto.UserInfoDTO;
import com.cc91.userservice.dto.UserProfileDTO;
import com.cc91.userservice.entity.User;
import com.cc91.userservice.entity.UserProfile;
import com.cc91.userservice.exception.BadRequestException;
import com.cc91.userservice.repository.UserProfileRepository;
import com.cc91.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;

    private static final BCryptPasswordEncoder VERIFY_ENCODER = new BCryptPasswordEncoder();

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userProfileRepository, VERIFY_ENCODER);
    }

    @Nested
    @DisplayName("getUserProfile()")
    class GetProfile {

        @Test
        @DisplayName("should return profile with empty fields when UserProfile not present")
        void shouldReturnEmptyProfileWhenNone() {
            User user = new User("alice", "a@b.com", "hash");
            user.setId(1L);
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

            UserProfileDTO dto = userService.getUserProfile("alice");
            assertEquals("alice", dto.getUsername());
            assertEquals("a@b.com", dto.getEmail());
            assertNull(dto.getAvatarUrl());
            assertNull(dto.getBio());
            assertEquals("USER", dto.getRole());
        }

        @Test
        @DisplayName("should return populated profile when UserProfile present")
        void shouldReturnPopulatedProfile() {
            User user = new User("alice", "a@b.com", "hash");
            user.setId(1L);
            UserProfile profile = new UserProfile(1L);
            profile.setAvatarUrl("https://cdn/avatar.png");
            profile.setBio("hi");
            profile.setLocation("SH");
            profile.setWebsite("https://alice.dev");
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

            UserProfileDTO dto = userService.getUserProfile("alice");
            assertEquals("https://cdn/avatar.png", dto.getAvatarUrl());
            assertEquals("hi", dto.getBio());
            assertEquals("SH", dto.getLocation());
            assertEquals("https://alice.dev", dto.getWebsite());
        }

        @Test
        @DisplayName("should throw RuntimeException when user does not exist")
        void shouldThrowWhenUserMissing() {
            when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> userService.getUserProfile("ghost"));
        }
    }

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfile {

        @Test
        @DisplayName("should only update provided (non-null) fields")
        void shouldUpdateProvidedFieldsOnly() {
            User user = new User("alice", "a@b.com", "hash");
            user.setId(1L);
            UserProfile existing = new UserProfile(1L);
            existing.setBio("old bio");
            existing.setLocation("old loc");
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(existing));

            UpdateUserProfileRequest req = new UpdateUserProfileRequest();
            req.setBio("new bio"); // only bio
            userService.updateProfile("alice", req);

            assertEquals("new bio", existing.getBio());
            assertEquals("old loc", existing.getLocation(), "location must be untouched");
            verify(userProfileRepository).save(existing);
        }

        @Test
        @DisplayName("should create new UserProfile if missing")
        void shouldCreateProfileIfMissing() {
            User user = new User("alice", "a@b.com", "hash");
            user.setId(1L);
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

            UpdateUserProfileRequest req = new UpdateUserProfileRequest();
            req.setAvatarUrl("https://cdn/x.png");
            userService.updateProfile("alice", req);

            verify(userProfileRepository).save(argThat(p ->
                    "https://cdn/x.png".equals(p.getAvatarUrl()) && p.getUser() == user));
        }
    }

    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("should throw BadRequest when old password is wrong")
        void shouldThrowWhenOldPasswordWrong() {
            User user = new User("alice", "a@b.com", VERIFY_ENCODER.encode("correct-old"));
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setOldPassword("wrong-old");
            req.setNewPassword("brand-new");

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> userService.changePassword("alice", req));
            assertEquals("旧密码不正确", ex.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should update password hash on correct old password")
        void shouldUpdatePasswordWhenOldCorrect() {
            User user = new User("alice", "a@b.com", VERIFY_ENCODER.encode("correct-old"));
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setOldPassword("correct-old");
            req.setNewPassword("brand-new-pw");
            userService.changePassword("alice", req);

            assertTrue(VERIFY_ENCODER.matches("brand-new-pw", user.getPasswordHash()));
            verify(userRepository).save(user);
        }
    }

    @Nested
    @DisplayName("getUserInfoById / getUserInfoByUsername")
    class UserInfoLookups {

        @Test
        @DisplayName("getUserInfoById should include avatarUrl from profile")
        void shouldIncludeAvatarUrlById() {
            User user = new User("alice", "a@b.com", "hash");
            user.setId(1L);
            user.setRole("ADMIN");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            UserProfile profile = new UserProfile(1L);
            profile.setAvatarUrl("https://cdn/a.png");
            when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

            UserInfoDTO info = userService.getUserInfoById(1L);
            assertEquals(1L, info.getId());
            assertEquals("alice", info.getUsername());
            assertEquals("ADMIN", info.getRole());
            assertEquals("https://cdn/a.png", info.getAvatarUrl());
        }

        @Test
        @DisplayName("getUserInfoById should have null avatarUrl when profile missing")
        void shouldReturnNullAvatarWhenNoProfile() {
            User user = new User("alice", "a@b.com", "hash");
            user.setId(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

            UserInfoDTO info = userService.getUserInfoById(1L);
            assertNull(info.getAvatarUrl());
        }

        @Test
        @DisplayName("getUserInfoByUsername should resolve user by username")
        void shouldResolveByUsername() {
            User user = new User("bob", "b@b.com", "hash");
            user.setId(2L);
            when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));
            when(userProfileRepository.findByUserId(2L)).thenReturn(Optional.empty());

            UserInfoDTO info = userService.getUserInfoByUsername("bob");
            assertEquals(2L, info.getId());
            assertEquals("bob", info.getUsername());
        }
    }

    @Nested
    @DisplayName("updateAvatarInternal()")
    class UpdateAvatarInternal {

        @Test
        @DisplayName("should update existing profile avatar URL")
        void shouldUpdateExistingProfileAvatar() {
            UserProfile profile = new UserProfile(1L);
            when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

            userService.updateAvatarInternal(1L, "https://cdn/new.png");

            assertEquals("https://cdn/new.png", profile.getAvatarUrl());
            verify(userProfileRepository).save(profile);
        }

        @Test
        @DisplayName("should create profile if it does not exist")
        void shouldCreateProfileWhenMissing() {
            User user = new User("alice", "a@b.com", "hash");
            user.setId(1L);
            when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            userService.updateAvatarInternal(1L, "https://cdn/new.png");

            verify(userProfileRepository).save(argThat(p ->
                    "https://cdn/new.png".equals(p.getAvatarUrl()) && p.getUser() == user));
        }
    }
}
