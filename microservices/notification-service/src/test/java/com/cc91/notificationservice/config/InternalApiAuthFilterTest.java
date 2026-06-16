package com.cc91.notificationservice.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * InternalApiAuthFilter unit tests.
 *
 * Filter matches on path.contains("/internal") so it intercepts both the
 * actual endpoint /api/notifications/internal (no trailing slash) and any
 * future paths under /internal/. Security surface: 401 on missing or wrong
 * X-Internal-Token, pass-through on public paths.
 */
class InternalApiAuthFilterTest {

    private static final String VALID_TOKEN = "valid-internal-token-cc91";

    private InternalApiAuthFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new InternalApiAuthFilter(VALID_TOKEN);
        chain = mock(FilterChain.class);
    }

    private MockHttpServletRequest request(String path, String method, String token) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI(path);
        req.setMethod(method);
        if (token != null) {
            req.addHeader("X-Internal-Token", token);
        }
        return req;
    }

    @Test
    @DisplayName("should NOT intercept paths without /internal/ (public paths pass through)")
    void shouldNotInterceptPublicPaths() throws Exception {
        MockHttpServletRequest req = request("/actuator/health", "GET", null);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, chain);

        assertEquals(200, res.getStatus());
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("should NOT intercept GET /api/notifications/unread-count")
    void shouldNotInterpretNotificationEndpoints() throws Exception {
        MockHttpServletRequest req = request("/api/notifications/unread-count", "GET", null);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
    }

    @Nested
    @DisplayName("internal endpoints (path contains /internal)")
    class InternalEndpoints {

        @Test
        @DisplayName("should pass through when X-Internal-Token matches (/internal/foo)")
        void shouldPassThroughWithValidToken() throws Exception {
            MockHttpServletRequest req = request("/internal/foo", "POST", VALID_TOKEN);
            MockHttpServletResponse res = new MockHttpServletResponse();

            filter.doFilterInternal(req, res, chain);

            assertEquals(200, res.getStatus());
            verify(chain).doFilter(req, res);
        }

        @Test
        @DisplayName("should intercept /api/notifications/internal (actual endpoint, no trailing slash)")
        void shouldInterceptActualEndpoint() throws Exception {
            MockHttpServletRequest req = request("/api/notifications/internal", "POST", null);
            MockHttpServletResponse res = new MockHttpServletResponse();

            filter.doFilterInternal(req, res, chain);

            assertEquals(401, res.getStatus());
            verify(chain, never()).doFilter(req, res);
        }

        @Test
        @DisplayName("should pass through /api/notifications/internal with valid token")
        void shouldPassActualEndpointWithValidToken() throws Exception {
            MockHttpServletRequest req = request("/api/notifications/internal", "POST", VALID_TOKEN);
            MockHttpServletResponse res = new MockHttpServletResponse();

            filter.doFilterInternal(req, res, chain);

            assertEquals(200, res.getStatus());
            verify(chain).doFilter(req, res);
        }

        @Test
        @DisplayName("should return 401 when X-Internal-Token header is missing")
        void shouldReturn401WhenTokenMissing() throws Exception {
            MockHttpServletRequest req = request("/internal/foo", "POST", null);
            MockHttpServletResponse res = new MockHttpServletResponse();

            filter.doFilterInternal(req, res, chain);

            assertEquals(401, res.getStatus());
            assertEquals("application/json;charset=UTF-8", res.getContentType());
            assertTrue(res.getContentAsString().contains("Invalid internal token"));
            verify(chain, never()).doFilter(req, res);
        }

        @Test
        @DisplayName("should return 401 when X-Internal-Token is wrong")
        void shouldReturn401WhenTokenWrong() throws Exception {
            MockHttpServletRequest req = request("/internal/foo", "POST", "wrong-token");
            MockHttpServletResponse res = new MockHttpServletResponse();

            filter.doFilterInternal(req, res, chain);

            assertEquals(401, res.getStatus());
            verify(chain, never()).doFilter(req, res);
        }

        @Test
        @DisplayName("should be case-sensitive on token comparison")
        void shouldBeCaseSensitive() throws Exception {
            MockHttpServletRequest req = request("/internal/foo", "POST", VALID_TOKEN.toUpperCase());
            MockHttpServletResponse res = new MockHttpServletResponse();

            filter.doFilterInternal(req, res, chain);

            assertEquals(401, res.getStatus());
            verify(chain, never()).doFilter(req, res);
        }
    }

    @Nested
    @DisplayName("empty configured token (defensive)")
    class EmptyConfiguredToken {

        @Test
        @DisplayName("should 401 any /internal/ request when configured token is empty")
        void shouldRejectAllWhenEmpty() throws Exception {
            InternalApiAuthFilter emptyFilter = new InternalApiAuthFilter("");
            MockHttpServletRequest req = request("/internal/foo", "POST", "anything");
            MockHttpServletResponse res = new MockHttpServletResponse();

            emptyFilter.doFilterInternal(req, res, chain);

            assertEquals(401, res.getStatus());
            verify(chain, never()).doFilter(req, res);
        }
    }
}
