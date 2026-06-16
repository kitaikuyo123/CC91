package com.cc91.contentservice.security;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter unit tests for content-service.
 * Verifies filter populates SecurityContext for valid tokens and never throws
 * (no auth bypass via exception leak).
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
    @DisplayName("should set SecurityContext when valid Bearer token present")
    void shouldSetContextOnValidToken() throws Exception {
        MockHttpServletRequest req = request("Bearer abc.def.ghi");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(jwtUtil.validateToken("abc.def.ghi")).thenReturn(true);
        when(jwtUtil.getUsernameFromToken("abc.def.ghi")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice"))
                .thenReturn(userDetails("alice", "USER"));

        filter.doFilterInternal(req, res, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("alice", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("should NOT set SecurityContext when no Authorization header")
    void shouldNotSetContextWithoutHeader() throws Exception {
        MockHttpServletRequest req = request(null);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("should NOT set SecurityContext when Authorization header lacks Bearer prefix")
    void shouldNotSetContextForNonBearerHeader() throws Exception {
        MockHttpServletRequest req = request("Basic abc");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("should NOT set SecurityContext when token is invalid")
    void shouldNotSetContextForInvalidToken() throws Exception {
        MockHttpServletRequest req = request("Bearer invalid");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(jwtUtil.validateToken("invalid")).thenReturn(false);

        filter.doFilterInternal(req, res, chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("should NEVER throw — exception in validateToken must be swallowed")
    void shouldSwallowExceptions() throws Exception {
        MockHttpServletRequest req = request("Bearer xyz");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        // Simulate the JJWT SignatureException previously leaking as 500.
        when(jwtUtil.validateToken("xyz")).thenThrow(new RuntimeException("JJWT SignatureException"));

        filter.doFilterInternal(req, res, chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("should NOT throw when userDetailsService throws (e.g. locked user)")
    void shouldSwallowUserDetailsServiceException() throws Exception {
        MockHttpServletRequest req = request("Bearer abc");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(jwtUtil.validateToken("abc")).thenReturn(true);
        when(jwtUtil.getUsernameFromToken("abc")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice"))
                .thenThrow(new RuntimeException("lock check failed"));

        filter.doFilterInternal(req, res, chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(req, res);
    }
}
