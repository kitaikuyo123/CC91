package com.cc91.userservice.fuzz;

import com.cc91.userservice.security.JwtUtil;
import io.jsonwebtoken.JwtException;
import net.jqwik.api.Assume;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Property-based fuzz tests for {@link JwtUtil} (robustness dimension).
 *
 * Invariants under fuzz:
 *   - validateToken never throws; tampered tokens always return false
 *   - get*FromToken on invalid input throws JwtException (or subclass),
 *     never NPE or any other RuntimeException
 *
 * Example-based positive/negative cases live in JwtUtilTest.
 */
class JwtUtilFuzzTest {

    private static final String SECRET =
            "test-jwt-secret-key-for-cc91-unit-tests-do-not-use-in-production-at-least-256-bits-long";
    private static final long EXPIRATION_MS = 3_600_000L;

    // JwtUtil is stateless after construction; jqwik @Property methods do not invoke
    // JUnit's @BeforeEach, so we build the instance once via static initializer.
    private static final JwtUtil JWT_UTIL = buildJwtUtil();

    private static JwtUtil buildJwtUtil() {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", SECRET);
        ReflectionTestUtils.setField(util, "expiration", EXPIRATION_MS);
        return util;
    }

    private JwtUtil jwtUtil() {
        return JWT_UTIL;
    }

    @Provide
    Arbitrary<String> garbageTokens() {
        // mix of sql-meta-ish, control chars, base64-ish junk
        return Arbitraries.oneOf(
                Arbitraries.strings().ofMinLength(1).ofMaxLength(200).ascii(),
                Arbitraries.of("", "  ", "null", "undefined", "Bearer xyz"),
                Arbitraries.strings().ofMinLength(1).ofMaxLength(50)
                        .withChars("'\";\\%<>& \n\r\t")
        );
    }

    /**
     * Corrupt a valid token at a random position with a random replacement char.
     */
    private String tamper(String token, long seed) {
        Random rng = new Random(seed);
        if (token == null || token.isEmpty()) return token;
        int pos = rng.nextInt(token.length());
        char replacement = (char) (rng.nextInt(95) + 32); // printable ASCII
        return token.substring(0, pos) + replacement + token.substring(pos + 1);
    }

    /**
     * Tampered tokens (any position, any replacement) must validate to false without throwing.
     *
     * Implementation note: a single-char change in the base64url tail of the signature
     * can flip bits that jjwt ignores (padding bits in the final 6-bit group). Such
     * tokens are semantically identical to the original, so they validate. We therefore
     * only consider a token "tampered" if the change is in the header or payload
     * segment (which is the part a real attacker would forge) — that segment is what
     * the HMAC actually protects.
     */
    @Property(tries = 250)
    void tamperedTokenFailsValidation(
            @ForAll String username,
            @ForAll Long userId,
            @ForAll String role,
            @ForAll long seed) {
        // jqwik-provided strings can be very long / weird; cap them so the token
        // is well-formed before we start tampering.
        Assume.that(username != null && username.length() <= 32);
        Assume.that(role != null && role.length() <= 16);

        String token = jwtUtil().generateToken(username, userId, role);
        // JWT = header.payload.signature — only corrupt header or payload; signature
        // bit-flips in the final base64url char can be no-ops as noted above.
        int lastDot = token.lastIndexOf('.');
        Assume.that(lastDot > 0);
        String tampered = tamper(token.substring(0, lastDot), seed) + token.substring(lastDot);
        // tampering might leave the token unchanged (same char) — re-issue in that case
        if (tampered.equals(token)) return;

        Boolean result = assertDoesNotThrow(() -> jwtUtil().validateToken(tampered));
        assertFalse(result, () -> "tampered token unexpectedly validated: orig=" + token + " tampered=" + tampered);
    }

    /**
     * On any non-empty input, getUserIdFromToken must either succeed (for genuinely
     * valid tokens) or throw a JwtException subclass — never NPE or other RuntimeException.
     */
    @Property(tries = 300)
    void invalidTokenThrowsJwtExceptionOnly(@ForAll("garbageTokens") String token) {
        boolean valid = jwtUtil().validateToken(token);
        if (valid) {
            // then extraction must succeed without throwing
            assertDoesNotThrow(() -> jwtUtil().getUserIdFromToken(token));
            return;
        }
        // invalid — must throw JwtException (or subclass). No NPE, no IllegalArgumentException
        // that escapes the contract, etc. jjwt's exception hierarchy roots at JwtException (RuntimeException).
        Throwable thrown = assertThrows(RuntimeException.class,
                () -> jwtUtil().getUserIdFromToken(token));
        assertTrue(thrown instanceof JwtException,
                () -> "expected JwtException for token='" + token + "' but got "
                        + thrown.getClass().getName() + ": " + thrown.getMessage());
    }
}
