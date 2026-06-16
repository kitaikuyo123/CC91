package com.cc91.forumservice.fuzz;

import com.cc91.forumservice.util.HtmlSanitizer;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Property-based fuzz tests for {@link HtmlSanitizer} (forum-service copy).
 *
 * Invariants checked here (robustness only):
 *   - sanitize never throws
 *   - output never carries an unescaped &lt;script&gt; substring
 *   - a second sanitize pass does not re-introduce HTML metacharacters
 *
 * Example-based behavior tests live in HtmlSanitizerTest.
 */
class HtmlSanitizerFuzzTest {

    /**
     * Characters that {@link org.springframework.web.util.HtmlUtils#htmlEscape}
     * actually transforms to entities. Used in the stability property.
     */
    private static final char[] DANGEROUS_CHARS = {
            '<', '>', '"', '\''
    };

    /** Superset for input-space generation (incl. space and ampersand). */
    private static final char[] GEN_CHARS = {
            '<', '>', '&', '"', '\'', '/', '\\', ' '
    };

    @Provide
    Arbitrary<String> randomStrings() {
        return Arbitraries.strings()
                .ofMinLength(0).ofMaxLength(10_000)
                .ascii();
    }

    @Provide
    Arbitrary<String> htmlStrings() {
        return Arbitraries.strings()
                .ofMinLength(0).ofMaxLength(5_000)
                .withChars(GEN_CHARS)
                .withChars("<script>")
                .withChars("</script>")
                .withChars("javascript:")
                .withChars("onerror=")
                .withChars("<img src=");
    }

    @Provide
    Arbitrary<String> scriptBearingStrings() {
        return Arbitraries.strings()
                .ofMinLength(0).ofMaxLength(2_000)
                .withChars(GEN_CHARS)
                .map(s -> "<script>" + s);
    }

    @Property(tries = 300)
    void sanitizeNeverThrows(@ForAll("randomStrings") String input) {
        assertDoesNotThrow(() -> HtmlSanitizer.sanitizeContent(input));
    }

    @Property(tries = 250)
    void sanitizeStripsRawScriptTag(@ForAll("scriptBearingStrings") String input) {
        String out = HtmlSanitizer.sanitizeContent(input);
        assertFalse(out.toLowerCase().contains("<script>"),
                () -> "sanitize leaked <script> for input: " + input);
    }

    /**
     * Stability: a second pass must not re-introduce any of the dangerous characters
     * that the first pass neutralized. Strict equality does NOT hold — see the
     * user-service version for the rationale.
     */
    @Property(tries = 200)
    void sanitizeSecondPassAddsNoDangerousChars(@ForAll("htmlStrings") String input) {
        String once = HtmlSanitizer.sanitizeContent(input);
        String twice = HtmlSanitizer.sanitizeContent(once);
        for (char c : DANGEROUS_CHARS) {
            assertFalse(twice.indexOf(c) >= 0,
                    () -> "second sanitize leaked dangerous char '" + c + "' for input: " + input);
        }
    }
}
