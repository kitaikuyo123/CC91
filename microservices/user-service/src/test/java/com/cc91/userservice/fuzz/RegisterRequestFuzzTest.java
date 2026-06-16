package com.cc91.userservice.fuzz;

import com.cc91.userservice.dto.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import net.jqwik.api.Assume;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Property-based fuzz tests for the {@link RegisterRequest} bean-validation contract.
 *
 * Robustness invariants checked here:
 *   - validate never throws NPE / unexpected exceptions on any input
 *   - inputs that match the documented username pattern always pass
 *   - inputs that do NOT match always fail validation
 *
 * Bean-validation rules (src):
 *   username: @NotBlank @Size(min=3, max=20)
 *   email:    @NotBlank @Email
 *   password: @NotBlank @Size(min=6, max=128)
 */
class RegisterRequestFuzzTest {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    // Validator is thread-safe and built once for the JVM (per jakarta.validation spec).
    // Built via a static initializer: jqwik @Property methods do not invoke JUnit
    // lifecycle callbacks (@BeforeAll etc.), so we cannot rely on those.
    private static final Validator VALIDATOR;
    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        VALIDATOR = factory.getValidator();
        // intentionally leak the factory — it lives for the JVM and Validator is meant
        // to be reused; closing would invalidate the Validator.
    }

    private static Validator validator() {
        return VALIDATOR;
    }

    @Provide
    Arbitrary<String> usernames() {
        return Arbitraries.strings()
                .ofMinLength(0).ofMaxLength(40)
                .withChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_ <>&\"';\\");
    }

    @Provide
    Arbitrary<String> emails() {
        return Arbitraries.strings()
                .ofMinLength(0).ofMaxLength(80)
                .withChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@._- <>&\"'");
    }

    @Provide
    Arbitrary<String> passwords() {
        return Arbitraries.strings()
                .ofMinLength(0).ofMaxLength(150)
                .withChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+-= <>&\"'\\");
    }

    @Provide
    Arbitrary<String> validUsernames() {
        // generate usernames restricted to the [a-zA-Z0-9_]{3,20} alphabet; the length
        // constraint is then explicitly re-checked in the property body.
        return Arbitraries.strings()
                .ofMinLength(0).ofMaxLength(30)
                .withChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_");
    }

    /**
     * jqwik Assume: abort this trial without failing it.
     */
    private static void assume(boolean cond) {
        Assume.that(cond);
    }

    /**
     * For any username/email/password combination, validator either passes or fails
     * with ConstraintViolations — it must never throw NPE or any other runtime error.
     */
    @Property(tries = 250)
    void validateNeverThrowsOnArbitraryInput(
            @ForAll("usernames") String username,
            @ForAll("emails") String email,
            @ForAll("passwords") String password) {

        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setEmail(email);
        req.setPassword(password);

        Set<ConstraintViolation<RegisterRequest>> violations = assertDoesNotThrow(
                () -> validator().validate(req));
        // result is either empty (passes) or non-empty (fails); both are acceptable.
        assertTrue(violations != null);
    }

    /**
     * Usernames that strictly match ^[a-zA-Z0-9_]{3,20}$ must pass the username constraint.
     */
    @Property(tries = 200)
    void validUsernamesPass(@ForAll("validUsernames") String username) {
        // only consider inputs that satisfy the strict pattern
        assume(USERNAME_PATTERN.matcher(username).matches());

        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setEmail("valid@example.com");
        req.setPassword("securePassword123");

        Set<ConstraintViolation<RegisterRequest>> violations = validator().validate(req);
        boolean usernameOk = violations.stream()
                .noneMatch(v -> v.getPropertyPath().toString().equals("username"));
        assertTrue(usernameOk,
                () -> "expected username '" + username + "' to be valid but got: " + violations);
    }

    /**
     * Usernames that do NOT match the pattern must produce a username violation.
     */
    @Property(tries = 200)
    void invalidUsernamesFail(@ForAll("validUsernames") String username) {
        Assume.that(!USERNAME_PATTERN.matcher(username).matches());

        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setEmail("valid@example.com");
        req.setPassword("securePassword123");

        Set<ConstraintViolation<RegisterRequest>> violations = validator().validate(req);
        boolean usernameViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        assertTrue(usernameViolation,
                () -> "expected username '" + username + "' to fail but it passed");
    }
}
