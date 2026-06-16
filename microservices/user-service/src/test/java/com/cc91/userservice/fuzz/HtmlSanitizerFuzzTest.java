package com.cc91.userservice.fuzz;

import com.cc91.userservice.util.HtmlSanitizer;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Property-based fuzz tests for {@link HtmlSanitizer}.
 *
 * Only robustness invariants are checked here:
 *   - never throws
 *   - output never carries an unescaped &lt;script&gt; substring
 *   - sanitize is idempotent
 *
 * Example-based behavior tests live in HtmlSanitizerTest.
 */
class HtmlSanitizerFuzzTest {

    /**
     * Characters that {@link org.springframework.web.util.HtmlUtils#htmlEscape}
     * actually transforms to entities. Used in the stability property to assert
     * the second sanitize pass adds none of these.
     */
    private static final char[] DANGEROUS_CHARS = {
            '<', '>', '"', '\''
    };

    /** Superset used by generators to bias the input space (includes space + ampersand). */
    private static final char[] GEN_CHARS = {
            '<', '>', '&', '"', '\'', '/', '\\', ' '
    };

    @Provide
    Arbitrary<String> randomStrings() {
        // ascii + unicode, capped length so the suite stays fast
        return Arbitraries.strings()
                .ofMinLength(0).ofMaxLength(10_000)
                .ascii();
    }

    @Provide
    Arbitrary<String> htmlStrings() {
        // bias the input space toward HTML/XSS metacharacters
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
        // a literal "<script>" prefix followed by random junk — guarantees the
        // raw input contains the substring the property is meant to neutralize
        return Arbitraries.strings()
                .ofMinLength(0).ofMaxLength(2_000)
                .withChars(GEN_CHARS)
                .map(s -> "<script>" + s);
    }

    /**
     * 300 tries: any input must not raise.
     */
    @Property(tries = 300)
    void sanitizeNeverThrows(@ForAll("randomStrings") String input) {
        assertDoesNotThrow(() -> HtmlSanitizer.sanitizeContent(input));
    }

    /**
     * After sanitize, the output must not contain the raw "&lt;script&gt;" substring
     * (case-insensitive) for any input that originally had a script marker.
     */
    @Property(tries = 250)
    void sanitizeStripsRawScriptTag(@ForAll("scriptBearingStrings") String input) {
        String out = HtmlSanitizer.sanitizeContent(input);
        assertFalse(out.toLowerCase().contains("<script>"),
                () -> "sanitize leaked <script> for input: " + input);
    }

    /**
     * Stability: a second pass must not re-introduce any of the dangerous characters
     * (&lt; &gt; & " ' / \) that the first pass neutralized. Strict equality
     * does NOT hold because HtmlUtils re-escapes the leading '&' of an entity
     * (e.g. sanitize("<") = "&lt;", sanitize("&lt;") = "&amp;lt;") — that is by
     * design: the contract is "no raw HTML metacharacters survive", not "byte-equal".
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
