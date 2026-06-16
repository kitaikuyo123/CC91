package com.cc91.userservice.base;

import com.cc91.userservice.config.SecurityConfig;
import com.cc91.userservice.security.JwtUtil;
import com.cc91.userservice.security.UserDetailsServiceImpl;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Shared base for @WebMvcTest classes in user-service.
 *
 * Subclasses MUST annotate themselves with
 * {@code @WebMvcTest(controllers = XxxController.class, ...)} so only the
 * controller under test is loaded (otherwise @WebMvcTest loads ALL controllers,
 * pulling in Repository / Service beans that are not mocked).
 *
 * Imports the real SecurityConfig so that path rules, @PreAuthorize, and the
 * InternalApiAuthFilter are exercised during slice tests (real filter chain runs).
 *
 * Mocks UserDetailsServiceImpl (which otherwise hits the DB) and JwtUtil.
 * The real JwtAuthenticationFilter is auto-wired by SecurityConfig; with no
 * Authorization header it is a no-op, so authenticated tests use @WithMockUser.
 */
@TestPropertySource(properties = {
        "cors.allowed-origins=http://localhost:5173",
        "cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS",
        "jwt.secret=test-jwt-secret-key-for-cc91-unit-tests-do-not-use-in-production-at-least-256-bits-long",
        "jwt.expiration=3600000",
        "jwt.refresh-expiration=604800000",
        "internal.token=test-internal-token-cc91",
        "account.lock.max-attempts=5",
        "account.lock.duration-seconds=30",
        "app.mail.console-log-only=true"
})
@Import(SecurityConfig.class)
public abstract class BaseWebMvcTest {

    @MockBean
    protected UserDetailsServiceImpl userDetailsService;

    @MockBean
    protected JwtUtil jwtUtil;
}
