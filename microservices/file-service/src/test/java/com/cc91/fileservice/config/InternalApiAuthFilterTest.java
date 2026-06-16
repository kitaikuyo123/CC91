package com.cc91.fileservice.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * InternalApiAuthFilter unit tests for file-service.
 *
 * The filter gates any path containing "/internal/" on the X-Internal-Token
 * header. file-service currently exposes no internal endpoints, but the filter
 * is wired into the SecurityConfig chain as the precedent for future service-to-
 * service APIs (and is already live in user/forum/content/notification-service).
 *
 * Coverage:
 * - non-internal paths always pass through (token irrelevant)
 * - /internal/ paths require the configured token
 * - empty internal.token config → reject every internal request (fail closed)
 * - token comparison must be exact (no partial / case variants)
 */
class InternalApiAuthFilterTest {

    private static final String CONFIGURED_TOKEN = "test-internal-token-cc91";

    private FilterChain chain;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        chain = mock(FilterChain.class);
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        // no shared state
    }

    private MockHttpServletRequest request(String method, String uri, String internalTokenHeader) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod(method);
        req.setRequestURI(uri);
        if (internalTokenHeader != null) {
            req.addHeader("X-Internal-Token", internalTokenHeader);
        }
        return req;
    }

    @Nested
    @DisplayName("when configured token is non-empty")
    class ConfiguredToken {

        private InternalApiAuthFilter filter;

        @BeforeEach
        void setUpFilter() {
            filter = new InternalApiAuthFilter(CONFIGURED_TOKEN);
        }

        @Test
        @DisplayName("non-internal paths pass through regardless of token")
        void nonInternalPathPassesThrough() throws Exception {
            MockHttpServletRequest req = request("GET", "/api/upload/avatar", null);
            filter.doFilterInternal(req, response, chain);
            assertEquals(200, response.getStatus());
            verify(chain).doFilter(any(), any());
        }

        @Test
        @DisplayName("/internal/ path with correct token passes through")
        void internalPathWithCorrectTokenPasses() throws Exception {
            MockHttpServletRequest req = request("GET", "/api/something/internal/foo", CONFIGURED_TOKEN);
            filter.doFilterInternal(req, response, chain);
            assertEquals(200, response.getStatus());
            verify(chain).doFilter(any(), any());
        }

        @Test
        @DisplayName("/internal/ path with missing token → 401 and chain NOT called")
        void internalPathWithMissingTokenIsUnauthorized() throws Exception {
            MockHttpServletRequest req = request("GET", "/api/internal/foo", null);
            filter.doFilterInternal(req, response, chain);
            assertEquals(401, response.getStatus());
            assertEquals("application/json;charset=UTF-8", response.getContentType());
            verify(chain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("/internal/ path with wrong token → 401 and chain NOT called")
        void internalPathWithWrongTokenIsUnauthorized() throws Exception {
            MockHttpServletRequest req = request("GET", "/api/internal/foo", "wrong-token");
            filter.doFilterInternal(req, response, chain);
            assertEquals(401, response.getStatus());
            verify(chain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("/internal/ path with token that is a substring of the configured one → 401 (exact match required)")
        void internalPathWithSubstringTokenIsUnauthorized() throws Exception {
            MockHttpServletRequest req = request("GET", "/api/internal/foo",
                    CONFIGURED_TOKEN.substring(0, 5));
            filter.doFilterInternal(req, response, chain);
            assertEquals(401, response.getStatus());
            verify(chain, never()).doFilter(any(), any());
        }
    }

    @Nested
    @DisplayName("when configured token is empty (fail closed)")
    class EmptyToken {

        private InternalApiAuthFilter filter;

        @BeforeEach
        void setUpFilter() {
            // Empty configured token — every internal request must be rejected
            filter = new InternalApiAuthFilter("");
        }

        @Test
        @DisplayName("/internal/ path is rejected even with a header value (fail closed)")
        void internalPathAlwaysRejected() throws Exception {
            MockHttpServletRequest req = request("GET", "/api/internal/foo", "anything");
            filter.doFilterInternal(req, response, chain);
            assertEquals(401, response.getStatus());
            verify(chain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("non-internal paths still pass through")
        void nonInternalPathStillPasses() throws Exception {
            MockHttpServletRequest req = request("GET", "/api/upload/images", null);
            filter.doFilterInternal(req, response, chain);
            assertEquals(200, response.getStatus());
            verify(chain).doFilter(any(), any());
        }
    }
}
