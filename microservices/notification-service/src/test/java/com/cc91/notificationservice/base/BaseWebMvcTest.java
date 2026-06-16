package com.cc91.notificationservice.base;

import com.cc91.notificationservice.config.SecurityConfig;
import com.cc91.notificationservice.security.JwtUtil;
import com.cc91.notificationservice.security.NoOpUserDetailsService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Shared base for @WebMvcTest classes in notification-service.
 *
 * Subclasses MUST annotate themselves with @WebMvcTest(controllers = XxxController.class).
 *
 * Imports the real SecurityConfig so path rules, the InternalApiAuthFilter, and
 * JwtAuthenticationFilter are exercised. Mocks NoOpUserDetailsService (otherwise
 * hits User Service via Feign) and JwtUtil.
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
    protected NoOpUserDetailsService userDetailsService;

    @MockBean
    protected JwtUtil jwtUtil;
}
