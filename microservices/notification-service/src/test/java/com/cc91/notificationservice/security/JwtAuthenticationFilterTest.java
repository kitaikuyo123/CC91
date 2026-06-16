package com.cc91.notificationservice.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter unit tests for notification-service.
 *
 * notification-service's filter differs from content/forum: it reads the role
 * claim from the JWT and builds authorities from it (defaulting to ROLE_USER).
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

    @Test
    @DisplayName("should set SecurityContext when valid Bearer token present")
    void shouldSetContextOnValidToken() throws Exception {
        MockHttpServletRequest req = request("Bearer abc");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(jwtUtil.validateToken("abc")).thenReturn(true);
        when(jwtUtil.getUsernameFromToken("abc")).thenReturn("alice");
        when(jwtUtil.getRoleFromToken("abc")).thenReturn("ADMIN");

        filter.doFilterInternal(req, res, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("alice", SecurityContextHolder.getContext().getAuthentication().getName());
        // ROLE_ADMIN derived from role claim
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        verify(chain).doFilter(req, res);
        verifyNoInteractions(userDetailsService); // notification filter builds UserDetails inline
    }

    @Test
    @DisplayName("should default to ROLE_USER when role claim is null")
    void shouldDefaultToUserWhenRoleNull() throws Exception {
        MockHttpServletRequest req = request("Bearer abc");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(jwtUtil.validateToken("abc")).thenReturn(true);
        when(jwtUtil.getUsernameFromToken("abc")).thenReturn("alice");
        when(jwtUtil.getRoleFromToken("abc")).thenReturn(null);

        filter.doFilterInternal(req, res, chain);

        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
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
        when(jwtUtil.validateToken("xyz")).thenThrow(new RuntimeException("JJWT SignatureException"));

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
}
