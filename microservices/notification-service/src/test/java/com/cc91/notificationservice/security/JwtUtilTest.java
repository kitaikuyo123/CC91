package com.cc91.notificationservice.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil unit tests for notification-service.
 *
 * CRITICAL regression coverage (OWASP A02/A07): validateToken must return false
 * (not throw) for a token signed with a different key. JJWT 0.12+ throws
 * io.jsonwebtoken.security.SignatureException which extends io.jsonwebtoken.security.SecurityException
 * (NOT java.lang.SecurityException). The previous `catch (SecurityException ex)` branch
 * matched java.lang.SecurityException and never caught the JJWT exception — causing 500.
 */
class JwtUtilTest {

    private static final String SECRET =
            "test-jwt-secret-key-for-cc91-unit-tests-do-not-use-in-production-at-least-256-bits-long";
    private static final long EXPIRATION_MS = 3600000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION_MS);
    }

    @Nested
    @DisplayName("generateToken / claim extraction")
    class GenerateAndExtract {

        @Test
        @DisplayName("should embed username as subject")
        void shouldEmbedUsername() {
            String token = jwtUtil.generateToken("alice");
            assertEquals("alice", jwtUtil.getUsernameFromToken(token));
        }

        @Test
        @DisplayName("getUserIdFromToken returns null (notification-service JwtUtil does not set userId claim)")
        void shouldReturnNullUserIdByDefault() {
            String token = jwtUtil.generateToken("alice");
            assertNull(jwtUtil.getUserIdFromToken(token));
        }

        @Test
        @DisplayName("getRoleFromToken returns null (notification-service JwtUtil does not set role claim)")
        void shouldReturnNullRoleByDefault() {
            String token = jwtUtil.generateToken("alice");
            assertNull(jwtUtil.getRoleFromToken(token));
        }
    }

    @Nested
    @DisplayName("validateToken")
    class Validate {

        @Test
        @DisplayName("should return true for valid token")
        void shouldReturnTrueForValidToken() {
            String token = jwtUtil.generateToken("alice");
            assertTrue(jwtUtil.validateToken(token));
        }

        @Test
        @DisplayName("REGRESSION: should return false (not throw/500) for token signed with a different key")
        void shouldReturnFalseForDifferentSigningKey() {
            JwtUtil other = new JwtUtil();
            ReflectionTestUtils.setField(other, "secret",
                    "completely-different-secret-that-is-long-enough-for-hmac-sha-256-padding");
            ReflectionTestUtils.setField(other, "expiration", EXPIRATION_MS);
            String foreignToken = other.generateToken("alice");
            assertFalse(jwtUtil.validateToken(foreignToken));
        }

        @Test
        @DisplayName("should return false for tampered token")
        void shouldReturnFalseForTamperedToken() {
            String token = jwtUtil.generateToken("alice");
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";
            assertFalse(jwtUtil.validateToken(tampered));
        }

        @Test
        @DisplayName("should return false for malformed token (garbage)")
        void shouldReturnFalseForMalformed() {
            assertFalse(jwtUtil.validateToken("not.a.real.token"));
        }

        @Test
        @DisplayName("should return false for null/empty input")
        void shouldReturnFalseForEmpty() {
            assertFalse(jwtUtil.validateToken(""));
            assertFalse(jwtUtil.validateToken(null));
        }

        @Test
        @DisplayName("should return false for expired token")
        void shouldReturnFalseForExpiredToken() {
            JwtUtil shortLived = new JwtUtil();
            ReflectionTestUtils.setField(shortLived, "secret", SECRET);
            ReflectionTestUtils.setField(shortLived, "expiration", -1000L);
            String expired = shortLived.generateToken("alice");
            assertFalse(jwtUtil.validateToken(expired));
        }

        @Test
        @DisplayName("should throw JwtException (subclass) when extracting claims from a tampered token")
        void shouldThrowWhenExtractingFromTampered() {
            String tampered = "garbage.token.value";
            assertThrows(JwtException.class, () -> jwtUtil.getUserIdFromToken(tampered));
            assertThrows(JwtException.class, () -> jwtUtil.getUsernameFromToken(tampered));
            assertThrows(JwtException.class, () -> jwtUtil.getRoleFromToken(tampered));
        }
    }

    @Nested
    @DisplayName("claim forgery attempt")
    class ClaimForgery {

        @Test
        @DisplayName("should not trust attacker-signed token")
        void shouldRejectAttackerSignedToken() {
            JwtUtil attacker = new JwtUtil();
            ReflectionTestUtils.setField(attacker, "secret",
                    "attacker-own-secret-key-long-enough-for-hmac-sha-key-padding-1234567890");
            ReflectionTestUtils.setField(attacker, "expiration", EXPIRATION_MS);
            String forged = attacker.generateToken("root");
            assertFalse(jwtUtil.validateToken(forged));
        }
    }

    @Test
    @DisplayName("getExpiration should return expiration in seconds")
    void shouldReturnExpirationInSeconds() {
        assertEquals(EXPIRATION_MS / 1000, jwtUtil.getExpiration());
    }
}
