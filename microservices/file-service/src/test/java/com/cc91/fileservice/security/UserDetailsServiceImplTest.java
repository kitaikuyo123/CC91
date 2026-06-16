package com.cc91.fileservice.security;

import com.cc91.fileservice.client.UserServiceClient;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserDetailsServiceImpl unit tests for file-service.
 *
 * file-service's UserDetailsServiceImpl reads the JWT from the request
 * Authorization header, extracts userId + role claims, and asks User Service
 * whether the account is locked (via Feign). Falls back to ROLE_USER when the
 * token or claims are missing, and to "unlocked" when the Feign call fails.
 */
class UserDetailsServiceImplTest {

    private static final String SECRET =
            "test-jwt-secret-key-for-cc91-unit-tests-do-not-use-in-production-at-least-256-bits-long";

    private UserServiceClient userServiceClient;
    private JwtUtil jwtUtil;
    private UserDetailsServiceImpl service;

    @BeforeEach
    void setUp() {
        userServiceClient = mock(UserServiceClient.class);
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);
        service = new UserDetailsServiceImpl(jwtUtil, userServiceClient);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void setBearerRequest(String bearerToken) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        if (bearerToken != null) {
            req.addHeader("Authorization", bearerToken);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String mint(String subject, Long userId, String role) {
        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000L))
                .signWith(signingKey());
        if (userId != null) builder.claim("userId", userId);
        if (role != null) builder.claim("role", role);
        return builder.compact();
    }

    @Test
    @DisplayName("should fall back to ROLE_USER when no token is present in request")
    void shouldFallbackToUserRoleWhenNoToken() {
        RequestContextHolder.resetRequestAttributes();
        UserDetails details = service.loadUserByUsername("alice");
        assertEquals("alice", details.getUsername());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        verifyNoInteractions(userServiceClient);
    }

    @Test
    @DisplayName("should fall back to ROLE_USER when token has no userId/role claims")
    void shouldFallbackWhenNoClaims() {
        String token = mint("alice", null, null);
        setBearerRequest("Bearer " + token);

        UserDetails details = service.loadUserByUsername("alice");
        assertEquals("alice", details.getUsername());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        verifyNoInteractions(userServiceClient);
    }

    @Test
    @DisplayName("should propagate role claim from token when present")
    void shouldPropagateRoleFromToken() {
        String token = mint("alice", 7L, "ADMIN");
        setBearerRequest("Bearer " + token);
        when(userServiceClient.isUserLocked(7L)).thenReturn(Map.of("locked", false));

        UserDetails details = service.loadUserByUsername("alice");
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("should return accountLocked=false when User Service says not locked")
    void shouldNotBeLocked() {
        String token = mint("alice", 7L, "USER");
        setBearerRequest("Bearer " + token);
        when(userServiceClient.isUserLocked(7L)).thenReturn(Map.of("locked", false));

        UserDetails details = service.loadUserByUsername("alice");
        assertTrue(details.isAccountNonLocked());
        verify(userServiceClient).isUserLocked(7L);
    }

    @Test
    @DisplayName("should return accountLocked=true when User Service says locked")
    void shouldBeLocked() {
        String token = mint("alice", 7L, "USER");
        setBearerRequest("Bearer " + token);
        when(userServiceClient.isUserLocked(7L)).thenReturn(Map.of("locked", true));

        UserDetails details = service.loadUserByUsername("alice");
        assertFalse(details.isAccountNonLocked());
    }

    @Test
    @DisplayName("should not throw and assume unlocked when User Service call fails")
    void shouldAssumeUnlockedOnUserServiceFailure() {
        String token = mint("alice", 7L, "USER");
        setBearerRequest("Bearer " + token);
        when(userServiceClient.isUserLocked(7L)).thenThrow(new RuntimeException("network"));

        UserDetails details = service.loadUserByUsername("alice");
        assertTrue(details.isAccountNonLocked());
        assertEquals("alice", details.getUsername());
    }

    @Test
    @DisplayName("should treat null 'locked' value from User Service as unlocked")
    void shouldTreatNullLockedAsUnlocked() {
        String token = mint("alice", 7L, "USER");
        setBearerRequest("Bearer " + token);
        when(userServiceClient.isUserLocked(7L)).thenReturn(Map.of());

        UserDetails details = service.loadUserByUsername("alice");
        assertTrue(details.isAccountNonLocked());
    }
}
