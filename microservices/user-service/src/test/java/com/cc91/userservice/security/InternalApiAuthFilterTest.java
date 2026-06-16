package com.cc91.userservice.security;

import com.cc91.userservice.config.InternalApiAuthFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * InternalApiAuthFilter unit tests.
 * Verifies X-Internal-Token enforcement on /internal/** paths
 * (OWASP A01 Broken Access Control).
 */
class InternalApiAuthFilterTest {

    private static final String VALID_TOKEN = "test-internal-token-cc91";
    private InternalApiAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new InternalApiAuthFilter(VALID_TOKEN);
    }

    private MockHttpServletRequest internalGet(String token) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/users/internal/1");
        req.setMethod("GET");
        if (token != null) {
            req.addHeader("X-Internal-Token", token);
        }
        return req;
    }

    private MockHttpServletRequest publicGet() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/users/alice");
        req.setMethod("GET");
        return req;
    }

    @Test
    @DisplayName("should allow request to non-internal path without token")
    void shouldAllowPublicPathWithoutToken() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(publicGet(), resp, chain);
        assertEquals(200, resp.getStatus());
        verify(chain).doFilter(any(), any());
    }

    @Test
    @DisplayName("should reject internal request when X-Internal-Token is missing")
    void shouldRejectWhenTokenMissing() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(internalGet(null), resp, chain);
        assertEquals(401, resp.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("should reject internal request when X-Internal-Token is wrong")
    void shouldRejectWhenTokenWrong() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(internalGet("wrong-token"), resp, chain);
        assertEquals(401, resp.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("should allow internal request when X-Internal-Token matches")
    void shouldAllowWhenTokenMatches() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(internalGet(VALID_TOKEN), resp, chain);
        assertEquals(200, resp.getStatus());
        verify(chain).doFilter(any(), any());
    }

    @Test
    @DisplayName("should reject ALL requests to /internal/ when configured token is empty")
    void shouldRejectWhenConfiguredTokenEmpty() throws Exception {
        InternalApiAuthFilter emptyTokenFilter = new InternalApiAuthFilter("");
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        // Even with a provided header, since configured token is empty -> reject
        emptyTokenFilter.doFilter(internalGet("anything"), resp, chain);
        assertEquals(401, resp.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }
}
