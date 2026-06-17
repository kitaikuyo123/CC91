package com.cc91.forumservice.base;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试用的自定义认证注解。
 *
 * <p>{@code @WithMockUser} 不会在 {@code Authentication.details} 中放入任何内容，
 * 但 forum-service 的 Controller（如 {@code PostController#getCurrentUserId()}）
 * 依赖 details 中的 "userId" 字段（由 {@code JwtAuthenticationFilter} 在生产环境
 * 从 JWT claims 写入）。本注解配合 {@link Factory} 在测试中重建一个带 userId
 * details 的 Authentication，让 Controller 切片测试能正确解析当前用户 id。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithJwtUser.Factory.class)
public @interface WithJwtUser {

    String username() default "alice";

    long userId() default 1L;

    String[] roles() default {"USER"};

    class Factory implements WithSecurityContextFactory<WithJwtUser> {
        @Override
        public SecurityContext createSecurityContext(WithJwtUser annotation) {
            SecurityContext context = SecurityContextHolder.createEmptyContext();

            UserDetails userDetails = User.withUsername(annotation.username())
                    .password("dummy")
                    .roles(annotation.roles())
                    .build();

            List<SimpleGrantedAuthority> authorities = userDetails.getAuthorities().stream()
                    .map(a -> new SimpleGrantedAuthority(a.getAuthority()))
                    .toList();

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, authorities);

            // 关键：把 userId 放进 details，与 JwtAuthenticationFilter 生产逻辑一致
            Map<String, Object> details = new HashMap<>();
            details.put("userId", annotation.userId());
            auth.setDetails(details);

            context.setAuthentication(auth);
            return context;
        }
    }
}
