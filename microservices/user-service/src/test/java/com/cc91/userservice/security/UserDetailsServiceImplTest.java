package com.cc91.userservice.security;

import com.cc91.userservice.entity.User;
import com.cc91.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserDetailsServiceImplTest {

    private UserRepository userRepository;
    private UserDetailsServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new UserDetailsServiceImpl(userRepository);
    }

    @Test
    @DisplayName("should throw UsernameNotFoundException when user does not exist")
    void shouldThrowWhenUserMissing() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("ghost"));
    }

    @Test
    @DisplayName("should map accountLocked=true when user is locked and lockUntil is null (banned)")
    void shouldMapAccountLockedWhenBanned() {
        User user = new User("alice", "a@b.com", "hash");
        user.setIsLocked(true);
        user.setLockUntil(null);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("alice");
        // Spring's User with accountLocked(true) is non-functional for login
        assertFalse(details.isAccountNonLocked());
        assertEquals("alice", details.getUsername());
    }

    @Test
    @DisplayName("should map accountLocked=true while lockUntil in future (still locked)")
    void shouldMapAccountLockedWhileLockActive() {
        User user = new User("alice", "a@b.com", "hash");
        user.setIsLocked(true);
        user.setLockUntil(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("alice");
        assertFalse(details.isAccountNonLocked());
    }

    @Test
    @DisplayName("should map accountLocked=false when isLocked=false")
    void shouldMapAccountNotLockedWhenUnlocked() {
        User user = new User("alice", "a@b.com", "hash");
        user.setIsLocked(false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("alice");
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("should include ADMIN role authority when role=ADMIN")
    void shouldIncludeAdminAuthority() {
        User user = new User("bob", "b@b.com", "hash");
        user.setRole("ADMIN");
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("bob");
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("should carry password hash from entity")
    void shouldCarryPasswordHash() {
        User user = new User("alice", "a@b.com", "bcrypt-hash-value");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("alice");
        assertEquals("bcrypt-hash-value", details.getPassword());
    }
}
