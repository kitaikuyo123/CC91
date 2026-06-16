package com.cc91.userservice.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * HtmlSanitizer unit tests.
 * Covers XSS escape behavior and null/empty edge cases (OWASP A03 Injection / XSS).
 */
class HtmlSanitizerTest {

    @Nested
    @DisplayName("escape(String)")
    class Escape {

        @Test
        @DisplayName("should escape '<' to &lt;")
        void shouldEscapeLessThan() {
            assertEquals("&lt;script&gt;", HtmlSanitizer.escape("<script>"));
        }

        @Test
        @DisplayName("should escape '>'")
        void shouldEscapeGreaterThan() {
            assertEquals("a&gt;b", HtmlSanitizer.escape("a>b"));
        }

        @Test
        @DisplayName("should escape '&' to &amp;")
        void shouldEscapeAmpersand() {
            assertEquals("a&amp;b", HtmlSanitizer.escape("a&b"));
        }

        @Test
        @DisplayName("should escape double quote")
        void shouldEscapeDoubleQuote() {
            assertEquals("&quot;x&quot;", HtmlSanitizer.escape("\"x\""));
        }

        @Test
        @DisplayName("should escape single quote")
        void shouldEscapeSingleQuote() {
            // Spring HtmlUtils escapes ' as &#39;
            assertEquals("&#39;x&#39;", HtmlSanitizer.escape("'x'"));
        }

        @Test
        @DisplayName("should neutralize <script> tag")
        void shouldNeutralizeScriptTag() {
            String payload = "<script>alert('xss')</script>";
            String result = HtmlSanitizer.escape(payload);
            // After escape, no live '<script>' substring should remain
            assertEquals(false, result.contains("<script>"));
            assertEquals(false, result.contains("</script>"));
        }

        @Test
        @DisplayName("should return null for null input")
        void shouldReturnNullForNull() {
            assertNull(HtmlSanitizer.escape(null));
        }

        @Test
        @DisplayName("should return empty for empty input")
        void shouldReturnEmptyForEmpty() {
            assertEquals("", HtmlSanitizer.escape(""));
        }

        @Test
        @DisplayName("should not alter plain text")
        void shouldNotAlterPlainText() {
            assertEquals("hello world 123", HtmlSanitizer.escape("hello world 123"));
        }
    }

    @Nested
    @DisplayName("sanitizeContent(String)")
    class SanitizeContent {

        @Test
        @DisplayName("should escape dangerous HTML in content")
        void shouldEscapeHtmlInContent() {
            String result = HtmlSanitizer.sanitizeContent("<img src=x onerror=alert(1)>");
            assertEquals(false, result.contains("<img"));
        }

        @Test
        @DisplayName("should return null for null input")
        void shouldReturnNullForNullContent() {
            assertNull(HtmlSanitizer.sanitizeContent(null));
        }

        @Test
        @DisplayName("should return empty for empty input")
        void shouldReturnEmptyForEmptyContent() {
            assertEquals("", HtmlSanitizer.sanitizeContent(""));
        }
    }
}
