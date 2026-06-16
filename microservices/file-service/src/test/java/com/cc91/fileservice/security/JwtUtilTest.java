package com.cc91.fileservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil unit tests for file-service.
 *
 * file-service's JwtUtil is verification-only (it never mints tokens — User Service
 * is the only issuer). To exercise the validate/extract paths we mint tokens locally
 * with the same secret using the JJWT builder directly.
 *
 * Critical coverage: validateToken must catch
 * {@link io.jsonwebtoken.security.SignatureException} (JJWT 0.12+) and return false
 * rather than bubbling up. Without that catch a forged token would crash the filter
 * chain with a 500 (OWASP A02 Cryptographic Failures, A07 Auth failures).
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

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /** Build a token signed with the test secret, with arbitrary claims. */
    private String mint(String subject, Long userId, String role, long ttlMs) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttlMs);
        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(exp)
                .signWith(signingKey());
        if (userId != null) builder.claim("userId", userId);
        if (role != null) builder.claim("role", role);
        return builder.compact();
    }

    @Nested
    @DisplayName("claim extraction")
    class ClaimExtraction {

        @Test
        @DisplayName("getUsernameFromToken returns the subject")
        void shouldExtractSubject() {
            String token = mint("alice", 7L, "USER", EXPIRATION_MS);
            assertEquals("alice", jwtUtil.getUsernameFromToken(token));
        }

        @Test
        @DisplayName("getUserIdFromToken returns the userId claim")
        void shouldExtractUserId() {
            String token = mint("alice", 7L, "USER", EXPIRATION_MS);
            assertEquals(7L, jwtUtil.getUserIdFromToken(token));
        }

        @Test
        @DisplayName("getRoleFromToken returns the role claim")
        void shouldExtractRole() {
            String token = mint("alice", 7L, "ADMIN", EXPIRATION_MS);
            assertEquals("ADMIN", jwtUtil.getRoleFromToken(token));
        }

        @Test
        @DisplayName("getUserIdFromToken returns null when claim is absent")
        void shouldReturnNullUserIdWhenAbsent() {
            String token = mint("alice", null, null, EXPIRATION_MS);
            assertNull(jwtUtil.getUserIdFromToken(token));
        }
    }

    @Nested
    @DisplayName("validateToken")
    class Validate {

        @Test
        @DisplayName("should return true for a valid token")
        void shouldReturnTrueForValidToken() {
            assertTrue(jwtUtil.validateToken(mint("alice", 1L, "USER", EXPIRATION_MS)));
        }

        @Test
        @DisplayName("should return false for null/empty input")
        void shouldReturnFalseForEmpty() {
            assertFalse(jwtUtil.validateToken(null));
            assertFalse(jwtUtil.validateToken(""));
        }

        @Test
        @DisplayName("should return false for malformed token")
        void shouldReturnFalseForMalformed() {
            assertFalse(jwtUtil.validateToken("not.a.real.token"));
        }

        @Test
        @DisplayName("should return false for expired token")
        void shouldReturnFalseForExpiredToken() {
            String expired = mint("alice", 1L, "USER", -1000L);
            assertFalse(jwtUtil.validateToken(expired));
        }

        @Test
        @DisplayName("should return false for token signed with a different key (signature mismatch)")
        void shouldReturnFalseForDifferentSigningKey() {
            // CRITICAL: this exercises the SignatureException catch. JJWT 0.12+ throws
            // io.jsonwebtoken.security.SignatureException for mismatched HMAC keys;
            // without the explicit catch the exception would propagate as 500.
            SecretKey foreignKey = Keys.hmacShaKeyFor(
                    ("completely-different-secret-that-is-long-enough-for-hmac-sha-256-padding")
                            .getBytes(StandardCharsets.UTF_8));
            String foreign = Jwts.builder()
                    .subject("alice")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                    .signWith(foreignKey)
                    .compact();
            assertFalse(jwtUtil.validateToken(foreign));
        }

        @Test
        @DisplayName("should return false for tampered token payload")
        void shouldReturnFalseForTamperedToken() {
            String token = mint("alice", 1L, "USER", EXPIRATION_MS);
            // Flip the last 5 chars of the signature
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";
            assertFalse(jwtUtil.validateToken(tampered));
        }
    }

    @Nested
    @DisplayName("claim forgery")
    class ClaimForgery {

        @Test
        @DisplayName("should reject attacker-signed token even if subject=root")
        void shouldRejectAttackerSignedToken() {
            SecretKey attackerKey = Keys.hmacShaKeyFor(
                    ("attacker-own-secret-key-long-enough-for-hmac-sha-key-padding-1234567890")
                            .getBytes(StandardCharsets.UTF_8));
            String forged = Jwts.builder()
                    .subject("root")
                    .claim("role", "ADMIN")
                    .claim("userId", 1L)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                    .signWith(attackerKey)
                    .compact();
            assertFalse(jwtUtil.validateToken(forged));
        }

        @Test
        @DisplayName("should throw JwtException when extracting claims from a tampered token")
        void shouldThrowWhenExtractingFromTampered() {
            assertThrows(JwtException.class, () -> jwtUtil.getUserIdFromToken("garbage.token.value"));
            assertThrows(JwtException.class, () -> jwtUtil.getUsernameFromToken("garbage.token.value"));
            assertThrows(JwtException.class, () -> jwtUtil.getRoleFromToken("garbage.token.value"));
        }
    }

    @Test
    @DisplayName("getExpiration should return expiration in seconds")
    void shouldReturnExpirationInSeconds() {
        assertEquals(EXPIRATION_MS / 1000, jwtUtil.getExpiration());
    }

    @Test
    @DisplayName("parser path can still extract Claims from a valid token (round-trip)")
    void shouldRoundTripClaimsFromValidToken() {
        String token = mint("bob", 42L, "ADMIN", EXPIRATION_MS);
        Claims parsed = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        assertEquals("bob", parsed.getSubject());
        assertEquals(42, parsed.get("userId", Integer.class).intValue());
    }
}
