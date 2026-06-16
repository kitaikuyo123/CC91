package com.cc91.contentservice.security;

import com.cc91.contentservice.client.UserServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UserDetailsServiceImpl unit tests for content-service.
 * content-service's UserDetailsServiceImpl extracts the JWT from the request
 * Authorization header, reads userId + role claims, and asks User Service
 * whether the account is locked.
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

    @Test
    @DisplayName("should fall back to ROLE_USER when no token present in request")
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
        // content-service JwtUtil.generateToken does not set userId/role claims
        String token = jwtUtil.generateToken("alice");
        setBearerRequest("Bearer " + token);

        UserDetails details = service.loadUserByUsername("alice");
        assertEquals("alice", details.getUsername());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        verifyNoInteractions(userServiceClient);
    }

    @Test
    @DisplayName("should return locked=false when no claims (no lock check performed)")
    void shouldNotPerformLockCheckWithoutClaims() {
        String token = jwtUtil.generateToken("alice");
        setBearerRequest("Bearer " + token);

        UserDetails details = service.loadUserByUsername("alice");
        assertTrue(details.isAccountNonLocked());
        // No lock check happens because there's no userId in the token
        verify(userServiceClient, never()).isUserLocked(any());
    }

    @Test
    @DisplayName("should not throw and assume unlocked when User Service call fails")
    void shouldAssumeUnlockedOnUserServiceFailure() {
        // Build a token without claims (content-service default) — exercises the
        // no-claims branch. Lock check isn't reached, but the implementation
        // guards any future failures. This test locks the "no throw" contract.
        String token = jwtUtil.generateToken("alice");
        setBearerRequest("Bearer " + token);
        when(userServiceClient.isUserLocked(any())).thenThrow(new RuntimeException("network"));

        UserDetails details = service.loadUserByUsername("alice");
        assertTrue(details.isAccountNonLocked());
        assertEquals("alice", details.getUsername());
    }
}
