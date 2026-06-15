package com.cc91.gateway;

import com.netflix.appinfo.InstanceInfo;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class GatewayDownstreamHealthIndicator implements ReactiveHealthIndicator {

    private static final List<String> REQUIRED_DOWNSTREAM_SERVICES = List.of(
            "user-service",
            "forum-service",
            "content-service",
            "notification-service",
            "file-service"
    );

    private final DiscoveryClient discoveryClient;

    public GatewayDownstreamHealthIndicator(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    public Mono<Health> health() {
        List<String> missing = REQUIRED_DOWNSTREAM_SERVICES.stream()
                .filter(svc -> discoveryClient.getInstances(svc).stream()
                        .noneMatch(GatewayDownstreamHealthIndicator::isInstanceUp))
                .toList();

        if (missing.isEmpty()) {
            return Mono.just(Health.up().build());
        }

        return Mono.just(Health.outOfService()
                .withDetails(Map.of(
                        "missingDownstreamServices", missing,
                        "detail", "Waiting for downstream services to register with Eureka as UP"
                ))
                .build());
    }

    private static boolean isInstanceUp(ServiceInstance instance) {
        if (instance instanceof EurekaServiceInstance eureka) {
            return eureka.getInstanceInfo().getStatus() == InstanceInfo.InstanceStatus.UP;
        }
        return true;
    }
}
