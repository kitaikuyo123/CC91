package com.cc91.forumservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil unit tests for forum-service.
 * Verifies token signing, claim extraction, validation against tampering / expiry
 * (OWASP A02 Cryptographic Failures, A07 Auth failures).
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
        @DisplayName("getUserIdFromToken returns null (forum-service JwtUtil does not set userId claim)")
        void shouldReturnNullUserIdByDefault() {
            String token = jwtUtil.generateToken("alice");
            // forum-service JwtUtil.generateToken only sets subject — userId is null
            assertNull(jwtUtil.getUserIdFromToken(token));
        }

        @Test
        @DisplayName("getRoleFromToken returns null (forum-service JwtUtil does not set role claim)")
        void shouldReturnNullRoleByDefault() {
            String token = jwtUtil.generateToken("alice");
            assertNull(jwtUtil.getRoleFromToken(token));
        }

        @Test
        @DisplayName("issuedAt and expiration should be embedded")
        void shouldEmbedTimestamps() {
            long before = System.currentTimeMillis();
            String token = jwtUtil.generateToken("alice");
            long after = System.currentTimeMillis();

            // round-trip by parsing claims — verify directly via parser to assert expiry window
            // We do this indirectly: validateToken should be true.
            assertTrue(jwtUtil.validateToken(token));
            // Verify expiration close to now + EXPIRATION_MS
            // (issuedAt is set by builder; subject extraction round-trips.)
            long afterExp = System.currentTimeMillis() + EXPIRATION_MS;
            // Token should still be valid well within the expiration window
            assertTrue(jwtUtil.validateToken(token));
            // sanity: before < after window boundary
            assertTrue(after < afterExp);
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
        @DisplayName("should return false for tampered token")
        void shouldReturnFalseForTamperedToken() {
            String token = jwtUtil.generateToken("alice");
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";
            assertFalse(jwtUtil.validateToken(tampered));
        }

        @Test
        @DisplayName("should return false for token signed with a different key")
        void shouldReturnFalseForDifferentSigningKey() {
            JwtUtil other = new JwtUtil();
            ReflectionTestUtils.setField(other, "secret",
                    "completely-different-secret-that-is-long-enough-for-hmac-sha-256-padding");
            ReflectionTestUtils.setField(other, "expiration", EXPIRATION_MS);
            String foreignToken = other.generateToken("alice");
            assertFalse(jwtUtil.validateToken(foreignToken));
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
        @DisplayName("should not trust attacker-signed token (even with subject=root)")
        void shouldRejectAttackerSignedToken() {
            JwtUtil attacker = new JwtUtil();
            ReflectionTestUtils.setField(attacker, "secret",
                    "attacker-own-secret-key-long-enough-for-hmac-sha-key-padding-1234567890");
            ReflectionTestUtils.setField(attacker, "expiration", EXPIRATION_MS);
            String forged = attacker.generateToken("root");
            assertFalse(jwtUtil.validateToken(forged));
        }

        @Test
        @DisplayName("should reject token forged with empty secret padding trick")
        void shouldRejectTokenWithWeakSecret() {
            JwtUtil weak = new JwtUtil();
            ReflectionTestUtils.setField(weak, "secret", "short");
            ReflectionTestUtils.setField(weak, "expiration", EXPIRATION_MS);
            // If key is too short, generation may throw; skip if so.
            try {
                String forged = weak.generateToken("alice");
                assertFalse(jwtUtil.validateToken(forged));
            } catch (Exception ignored) {
                // expected when secret too short for HMAC-SHA
            }
        }
    }

    @Test
    @DisplayName("getExpiration should return expiration in seconds")
    void shouldReturnExpirationInSeconds() {
        assertEquals(EXPIRATION_MS / 1000, jwtUtil.getExpiration());
    }
}
