package com.cc91.eureka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test for {@link EurekaServerApplication}.
 *
 * Verifies the {@code @EnableEurekaServer} application context loads without
 * errors. The "test" profile (see src/test/resources/application-test.yml)
 * disables register-with-eureka and fetch-registry so the context does not
 * attempt to contact a peer Eureka node.
 */
@SpringBootTest
@ActiveProfiles("test")
class EurekaServerApplicationTest {

    @Test
    @DisplayName("application context loads successfully")
    void contextLoads() {
        // Spring Boot loads the context; an empty body verifies success
        // (any startup failure surfaces as a context-load error).
    }
}
