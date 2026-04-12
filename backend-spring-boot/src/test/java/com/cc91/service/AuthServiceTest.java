package com.cc91.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuthService 集成测试
 * 使用 H2 内存数据库进行测试
 * 注意：注册流程只发送验证码，不创建用户，所以用户名重复检查不适用
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Test
    void authService_IsNotNull() {
        assertNotNull(authService);
    }

    @Test
    void authService_IsAutowired() {
        // 验证 AuthService 可以正常注入
        assertNotNull(authService, "AuthService should be autowired");
    }

    @Test
    void authService_IsInstanceOfAuthService() {
        // 验证注入的是正确的类型（考虑 Spring 代理）
        assertTrue(authService instanceof AuthService, "Should be instance of AuthService");
    }

    @Test
    void register_ValidData_ReturnsResponse() {
        // 测试注册功能（需要 H2 数据库）
        com.cc91.dto.RegisterRequest request = new com.cc91.dto.RegisterRequest();
        request.setUsername("newuser" + System.currentTimeMillis());
        request.setEmail("test" + System.currentTimeMillis() + "@example.com");
        request.setPassword("password123");

        assertDoesNotThrow(() -> {
            com.cc91.dto.RegisterResponse response = authService.register(request);
            assertNotNull(response);
            assertEquals("验证码已发送至邮箱", response.getMessage());
            assertEquals(600, response.getExpiresIn());
        });
    }

    @Test
    void register_MultipleRequestsForSameEmail_Succeeds() {
        // 注意：当前注册流程只发送验证码，不创建用户
        // 所以多次注册同一邮箱是允许的（每次生成新的验证码）
        String email = "test@example.com";

        com.cc91.dto.RegisterRequest request1 = new com.cc91.dto.RegisterRequest();
        request1.setUsername("user1");
        request1.setEmail(email);
        request1.setPassword("password123");

        com.cc91.dto.RegisterRequest request2 = new com.cc91.dto.RegisterRequest();
        request2.setUsername("user2");
        request2.setEmail(email);
        request2.setPassword("password456");

        // 两次请求都应该成功（因为只创建验证码）
        assertDoesNotThrow(() -> {
            com.cc91.dto.RegisterResponse response1 = authService.register(request1);
            assertNotNull(response1);
            assertEquals("验证码已发送至邮箱", response1.getMessage());

            com.cc91.dto.RegisterResponse response2 = authService.register(request2);
            assertNotNull(response2);
            assertEquals("验证码已发送至邮箱", response2.getMessage());
        });
    }
}
