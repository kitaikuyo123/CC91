package com.cc91.fileservice.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter unit tests for file-service.
 * Verifies the filter only populates SecurityContext for a valid Bearer token
 * and never throws (no auth bypass via exception leak).
 */
class JwtAuthenticationFilterTest {

    private JwtUtil jwtUtil;
    private UserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtUtil = mock(JwtUtil.class);
        userDetailsService = mock(UserDetailsService.class);
        filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("should not authenticate when token is invalid (jwtUtil.validateToken=false)")
    void shouldNotAuthenticateWhenTokenInvalid() throws Exception {
        when(jwtUtil.validateToken("garbage")).thenReturn(false);
        FilterChain chain = mock(FilterChain.class);
        filter.doFilterInternal(request("Bearer garbage"), new MockHttpServletResponse(), chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
        verify(jwtUtil, never()).getUsernameFromToken(anyString());
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("should set SecurityContext when token is valid")
    void shouldSetContextWhenTokenValid() throws Exception {
        String token = "valid.jwt.token";
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(token)).thenReturn("alice");
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
        String token = "valid.jwt.token";
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(token)).thenReturn("ghost");
        when(userDetailsService.loadUserByUsername("ghost"))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("nope"));

        FilterChain chain = mock(FilterChain.class);
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
        verifyNoInteractions(jwtUtil, userDetailsService);
    }

    @Test
    @DisplayName("should not authenticate when Authorization header has only 'Bearer' (no token)")
    void shouldNotAuthenticateWhenBearerOnly() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        filter.doFilterInternal(request("Bearer "), new MockHttpServletResponse(), chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
        verifyNoInteractions(jwtUtil, userDetailsService);
    }

    @Test
    @DisplayName("should not authenticate when jwtUtil.validateToken throws (defensive)")
    void shouldNotAuthenticateWhenValidateTokenThrows() throws Exception {
        when(jwtUtil.validateToken(anyString())).thenThrow(new RuntimeException("boom"));
        FilterChain chain = mock(FilterChain.class);
        filter.doFilterInternal(request("Bearer xyz"), new MockHttpServletResponse(), chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
    }
}
