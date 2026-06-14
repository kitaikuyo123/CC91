package com.cc91.forumservice.security;

import com.cc91.forumservice.client.UserServiceClient;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Resolves UserDetails from JWT + checks lock status via User Service.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    private final JwtUtil jwtUtil;
    private final UserServiceClient userServiceClient;

    public UserDetailsServiceImpl(JwtUtil jwtUtil, UserServiceClient userServiceClient) {
        this.jwtUtil = jwtUtil;
        this.userServiceClient = userServiceClient;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String token = extractCurrentToken();
        if (token != null) {
            Long userId = jwtUtil.getUserIdFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);
            if (userId != null && role != null) {
                boolean locked = checkLocked(userId);
                return org.springframework.security.core.userdetails.User.builder()
                        .username(username)
                        .password("")
                        .roles(role)
                        .accountLocked(locked)
                        .build();
            }
        }
        return org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password("")
                .roles("USER")
                .build();
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

    private String extractCurrentToken() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            String bearer = request.getHeader("Authorization");
            if (bearer != null && bearer.startsWith("Bearer ")) {
                return bearer.substring(7);
            }
        }
        return null;
    }
}
