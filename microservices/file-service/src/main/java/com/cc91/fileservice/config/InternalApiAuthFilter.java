package com.cc91.fileservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Validates X-Internal-Token header for internal API endpoints.
 * Prevents unauthenticated access to service-to-service endpoints
 * when bypassing the gateway.
 */
@Component
public class InternalApiAuthFilter extends OncePerRequestFilter {

    private final String internalToken;

    public InternalApiAuthFilter(@Value("${internal.token:${INTERNAL_TOKEN:}}") String internalToken) {
        this.internalToken = internalToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.contains("/internal/")) {
            String token = request.getHeader("X-Internal-Token");
            if (internalToken.isEmpty() || !internalToken.equals(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"Invalid internal token\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
