package com.cc91.fileservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Feign request interceptor that attaches the internal API token header
 * to all outgoing service-to-service calls.
 */
@Component
public class FeignConfig implements RequestInterceptor {

    private final String internalToken;

    public FeignConfig(@Value("${internal.token:${INTERNAL_TOKEN:}}") String internalToken) {
        this.internalToken = internalToken;
    }

    @Override
    public void apply(RequestTemplate template) {
        if (!internalToken.isEmpty()) {
            template.header("X-Internal-Token", internalToken);
        }
    }
}
