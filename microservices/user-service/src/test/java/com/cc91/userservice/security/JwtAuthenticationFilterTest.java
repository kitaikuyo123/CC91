package com.cc91.userservice.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter unit tests.
 * Verifies that the filter only populates SecurityContext for a valid Bearer
 * token and never throws (no auth bypass via exception leak).
 */
class JwtAuthenticationFilterTest {

    private JwtUtil jwtUtil;
    private UserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;

    private static final String SECRET =
            "test-jwt-secret-key-for-cc91-unit-tests-do-not-use-in-production-at-least-256-bits-long";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);
        userDetailsService = mock(UserDetailsService.class);
        filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
    }

    private MockHttpServletRequest request(String authHeader) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        if (authHeader != null) {
            req.addHeader("Authorization", authHeader);
        }
        return req;
    }

    private UserDetails userDetails(String username, String role) {
        return User.builder()
                .username(username)
                .password("ignored")
                .roles(role)
                .build();
    }

    @Test
    @DisplayName("should not authenticate when no Authorization header present")
    void shouldNotAuthenticateWhenNoAuthHeader() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        filter.doFilterInternal(request(null), new MockHttpServletResponse(), chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
    }

    @Test
    @DisplayName("should not authenticate when Authorization header lacks Bearer prefix")
    void shouldNotAuthenticateWhenNoBearerPrefix() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        filter.doFilterInternal(request("Basic somevalue"), new MockHttpServletResponse(), chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    @DisplayName("should not authenticate when token is invalid (tampered)")
    void shouldNotAuthenticateWhenTokenInvalid() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        filter.doFilterInternal(request("Bearer garbage.token.value"), new MockHttpServletResponse(), chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    @DisplayName("should set SecurityContext when token is valid")
    void shouldSetContextWhenTokenValid() throws Exception {
        String token = jwtUtil.generateToken("alice", 1L, "USER");
        when(userDetailsService.loadUserByUsername("alice"))
                .thenReturn(userDetails("alice", "USER"));

        FilterChain chain = mock(FilterChain.class);
        filter.doFilterInternal(request("Bearer " + token), new MockHttpServletResponse(), chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("alice", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(chain).doFilter(any(), any());
    }

    @Test
    @DisplayName("should not throw and should not authenticate when UserDetailsService fails")
    void shouldNotThrowWhenUserDetailsServiceFails() throws Exception {
        String token = jwtUtil.generateToken("ghost", 1L, "USER");
        when(userDetailsService.loadUserByUsername("ghost"))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("nope"));

        FilterChain chain = mock(FilterChain.class);
        // must not throw
        filter.doFilterInternal(request("Bearer " + token), new MockHttpServletResponse(), chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
    }

    @Test
    @DisplayName("should continue chain (do not abort) even when no token present")
    void shouldContinueChainEvenWithoutToken() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        filter.doFilterInternal(request(null), new MockHttpServletResponse(), chain);
        verify(chain).doFilter(any(), any());
        verifyNoInteractions(userDetailsService);
    }
}
