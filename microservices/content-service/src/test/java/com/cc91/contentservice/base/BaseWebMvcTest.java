package com.cc91.contentservice.base;

import com.cc91.contentservice.config.SecurityConfig;
import com.cc91.contentservice.security.JwtUtil;
import com.cc91.contentservice.security.UserDetailsServiceImpl;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Shared base for @WebMvcTest classes in content-service.
 *
 * Subclasses MUST annotate themselves with
 * {@code @WebMvcTest(controllers = XxxController.class)} so only the
 * controller under test is loaded.
 *
 * Imports the real SecurityConfig so that path rules, @PreAuthorize, and the
 * JwtAuthenticationFilter are exercised during slice tests. The fix in
 * GlobalExceptionHandler (explicit AccessDeniedException → 403) is therefore
 * exercised too.
 *
 * Mocks UserDetailsServiceImpl (which otherwise hits User Service via Feign)
 * and JwtUtil. Tests use @WithMockUser to drive authentication.
 */
@TestPropertySource(properties = {
        "cors.allowed-origins=http://localhost:5173",
        "cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS",
        "jwt.secret=test-jwt-secret-key-for-cc91-unit-tests-do-not-use-in-production-at-least-256-bits-long",
        "jwt.expiration=3600000",
        "internal.token=test-internal-token-cc91"
})
@Import(SecurityConfig.class)
public abstract class BaseWebMvcTest {

    @MockBean
    protected UserDetailsServiceImpl userDetailsService;

    @MockBean
    protected JwtUtil jwtUtil;
}
