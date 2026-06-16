package com.cc91.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test for {@link GatewayApplication}.
 *
 * Verifies the WebFlux + Spring Cloud Gateway application context loads without
 * errors (auto-configuration, route definitions, actuator, micrometer, the
 * {@link GatewayDownstreamHealthIndicator} bean).
 *
 * The "test" profile disables Eureka registration / registry-fetch so the
 * context does not require a live Eureka server.
 */
@SpringBootTest
@ActiveProfiles("test")
class GatewayApplicationTest {

    @Test
    @DisplayName("application context loads successfully")
    void contextLoads() {
        // Spring Boot loads the context; an empty test body verifies success
        // (any startup failure surfaces as a context-load error).
    }
}
