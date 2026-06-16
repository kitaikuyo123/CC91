package com.cc91.gateway;

import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link GatewayDownstreamHealthIndicator} unit tests.
 *
 * The indicator queries {@link DiscoveryClient} for each of the 5 required
 * downstream services and reports Health.up() only when every one has at least
 * one UP instance. Otherwise it reports Health.outOfService() with the list of
 * missing services in the details. Missing = (a) no instances at all, or (b)
 * instances present but in non-UP Eureka status.
 *
 * Uses pure Mockito — no Spring context required (the indicator is a plain
 * {@code @Component} that depends on DiscoveryClient).
 */
class GatewayDownstreamHealthIndicatorTest {

    private DiscoveryClient discoveryClient;
    private GatewayDownstreamHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        discoveryClient = mock(DiscoveryClient.class);
        indicator = new GatewayDownstreamHealthIndicator(discoveryClient);
    }

    /** A non-Eureka ServiceInstance — should be treated as UP (default branch). */
    private ServiceInstance plainInstance(String serviceId) {
        return new DefaultServiceInstance(serviceId + "-1", serviceId, "localhost", 8080, false);
    }

    /** A Eureka-backed instance in the given Eureka status. */
    private ServiceInstance eurekaInstance(String serviceId, InstanceInfo.InstanceStatus status) {
        InstanceInfo info = InstanceInfo.Builder.newBuilder()
                .setAppName(serviceId)
                .setStatus(status)
                .build();
        return new EurekaServiceInstance(info);
    }

    /**
     * Configure DiscoveryClient to return the given instances per service.
     * Services not mentioned get an empty list.
     */
    private void stubInstances(Map<String, List<ServiceInstance>> perService) {
        when(discoveryClient.getInstances(anyString())).thenAnswer(inv -> {
            String svc = inv.getArgument(0);
            return perService.getOrDefault(svc, List.of());
        });
    }

    private List<String> missingServices(Health health) {
        @SuppressWarnings("unchecked")
        List<String> missing = (List<String>) health.getDetails().get("missingDownstreamServices");
        return missing;
    }

    @Nested
    @DisplayName("when all required services are UP")
    class AllUp {

        @Test
        @DisplayName("should emit Health.up() when all 5 services have an UP Eureka instance")
        void shouldReturnUpWhenAllServicesUp() {
            stubInstances(Map.of(
                    "user-service", List.of(eurekaInstance("user-service", InstanceInfo.InstanceStatus.UP)),
                    "forum-service", List.of(eurekaInstance("forum-service", InstanceInfo.InstanceStatus.UP)),
                    "content-service", List.of(eurekaInstance("content-service", InstanceInfo.InstanceStatus.UP)),
                    "notification-service", List.of(eurekaInstance("notification-service", InstanceInfo.InstanceStatus.UP)),
                    "file-service", List.of(eurekaInstance("file-service", InstanceInfo.InstanceStatus.UP))
            ));

            Mono<Health> result = indicator.health();
            Health h = result.block();
            assertNotNull(h);
            assertEquals(Status.UP, h.getStatus());
            assertNull(h.getDetails().get("missingDownstreamServices"));
        }

        @Test
        @DisplayName("should treat non-Eureka ServiceInstance as UP (default branch)")
        void shouldTreatPlainInstanceAsUp() {
            stubInstances(Map.of(
                    "user-service", List.of(plainInstance("user-service")),
                    "forum-service", List.of(plainInstance("forum-service")),
                    "content-service", List.of(plainInstance("content-service")),
                    "notification-service", List.of(plainInstance("notification-service")),
                    "file-service", List.of(plainInstance("file-service"))
            ));

            Health h = indicator.health().block();
            assertNotNull(h);
            assertEquals(Status.UP, h.getStatus());
        }

        @Test
        @DisplayName("should return UP when each service has at least one UP among several instances")
        void shouldReturnUpWhenAtLeastOneUpInstance() {
            stubInstances(Map.of(
                    "user-service", List.of(
                            eurekaInstance("user-service", InstanceInfo.InstanceStatus.DOWN),
                            eurekaInstance("user-service", InstanceInfo.InstanceStatus.UP)),
                    "forum-service", List.of(eurekaInstance("forum-service", InstanceInfo.InstanceStatus.UP)),
                    "content-service", List.of(eurekaInstance("content-service", InstanceInfo.InstanceStatus.UP)),
                    "notification-service", List.of(eurekaInstance("notification-service", InstanceInfo.InstanceStatus.UP)),
                    "file-service", List.of(eurekaInstance("file-service", InstanceInfo.InstanceStatus.UP))
            ));

            Health h = indicator.health().block();
            assertNotNull(h);
            assertEquals(Status.UP, h.getStatus());
        }
    }

    @Nested
    @DisplayName("when some services are missing")
    class SomeMissing {

        @Test
        @DisplayName("should emit OUT_OF_SERVICE and list a single missing service")
        void shouldReturnOutOfServiceForOneMissing() {
            stubInstances(Map.of(
                    "user-service", List.of(eurekaInstance("user-service", InstanceInfo.InstanceStatus.UP)),
                    "forum-service", List.of(eurekaInstance("forum-service", InstanceInfo.InstanceStatus.UP)),
                    "content-service", List.of(eurekaInstance("content-service", InstanceInfo.InstanceStatus.UP)),
                    "notification-service", List.of(eurekaInstance("notification-service", InstanceInfo.InstanceStatus.UP))
                    // file-service intentionally missing
            ));

            Health h = indicator.health().block();
            assertNotNull(h);
            assertEquals(Status.OUT_OF_SERVICE, h.getStatus());
            List<String> missing = missingServices(h);
            assertNotNull(missing);
            assertEquals(List.of("file-service"), missing);
            assertNotNull(h.getDetails().get("detail"));
        }

        @Test
        @DisplayName("should list multiple missing services")
        void shouldReturnOutOfServiceForMultipleMissing() {
            stubInstances(Map.of(
                    "user-service", List.of(eurekaInstance("user-service", InstanceInfo.InstanceStatus.UP))
                    // forum/content/notification/file all missing
            ));

            Health h = indicator.health().block();
            assertNotNull(h);
            assertEquals(Status.OUT_OF_SERVICE, h.getStatus());
            List<String> missing = missingServices(h);
            assertNotNull(missing);
            assertEquals(4, missing.size());
            assertTrue(missing.contains("forum-service"));
            assertTrue(missing.contains("content-service"));
            assertTrue(missing.contains("notification-service"));
            assertTrue(missing.contains("file-service"));
        }

        @Test
        @DisplayName("should treat instance present-but-DOWN as missing")
        void shouldTreatDownInstanceAsMissing() {
            stubInstances(Map.of(
                    "user-service", List.of(eurekaInstance("user-service", InstanceInfo.InstanceStatus.DOWN)),
                    "forum-service", List.of(eurekaInstance("forum-service", InstanceInfo.InstanceStatus.UP)),
                    "content-service", List.of(eurekaInstance("content-service", InstanceInfo.InstanceStatus.UP)),
                    "notification-service", List.of(eurekaInstance("notification-service", InstanceInfo.InstanceStatus.UP)),
                    "file-service", List.of(eurekaInstance("file-service", InstanceInfo.InstanceStatus.UP))
            ));

            Health h = indicator.health().block();
            assertNotNull(h);
            assertEquals(Status.OUT_OF_SERVICE, h.getStatus());
            List<String> missing = missingServices(h);
            assertEquals(List.of("user-service"), missing);
        }

        @Test
        @DisplayName("should return OUT_OF_SERVICE when DiscoveryClient returns empty lists for everything")
        void shouldReturnOutOfServiceWhenAllEmpty() {
            when(discoveryClient.getInstances(anyString())).thenReturn(List.of());

            Health h = indicator.health().block();
            assertNotNull(h);
            assertEquals(Status.OUT_OF_SERVICE, h.getStatus());
            List<String> missing = missingServices(h);
            assertNotNull(missing);
            assertEquals(5, missing.size());
        }
    }

    @Test
    @DisplayName("should query DiscoveryClient exactly once per required service (no redundant calls)")
    void shouldQueryEachServiceOnce() {
        stubInstances(Map.of(
                "user-service", List.of(eurekaInstance("user-service", InstanceInfo.InstanceStatus.UP)),
                "forum-service", List.of(eurekaInstance("forum-service", InstanceInfo.InstanceStatus.UP)),
                "content-service", List.of(eurekaInstance("content-service", InstanceInfo.InstanceStatus.UP)),
                "notification-service", List.of(eurekaInstance("notification-service", InstanceInfo.InstanceStatus.UP)),
                "file-service", List.of(eurekaInstance("file-service", InstanceInfo.InstanceStatus.UP))
        ));

        indicator.health().block();

        verify(discoveryClient).getInstances("user-service");
        verify(discoveryClient).getInstances("forum-service");
        verify(discoveryClient).getInstances("content-service");
        verify(discoveryClient).getInstances("notification-service");
        verify(discoveryClient).getInstances("file-service");
    }
}
