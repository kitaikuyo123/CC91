package com.cc91.notificationservice.security;

import com.cc91.notificationservice.client.UserServiceClient;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * JWT UserDetailsService for Notification Service.
 * Validates JWT and checks user lock status via User Service.
 */
@Component
public class NoOpUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(NoOpUserDetailsService.class);

    private final JwtUtil jwtUtil;
    private final UserServiceClient userServiceClient;

    public NoOpUserDetailsService(JwtUtil jwtUtil, UserServiceClient userServiceClient) {
        this.jwtUtil = jwtUtil;
        this.userServiceClient = userServiceClient;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Long userId = extractUserIdFromToken();
        boolean locked = userId != null && checkLocked(userId);
        return User.withUsername(username)
                .password("")
                .roles("USER")
                .accountLocked(locked)
                .build();
    }

    private Long extractUserIdFromToken() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            String bearer = request.getHeader("Authorization");
            if (bearer != null && bearer.startsWith("Bearer ")) {
                return jwtUtil.getUserIdFromToken(bearer.substring(7));
            }
        }
        return null;
    }

    private boolean checkLocked(Long userId) {
        try {
            Map<String, Boolean> result = userServiceClient.isUserLocked(userId);
            return result != null && Boolean.TRUE.equals(result.get("locked"));
        } catch (Exception e) {
            logger.warn("Failed to check lock status for userId={}, assuming unlocked: {}", userId, e.getMessage());
            return false;
        }
    }
}
