package com.cc91.fileservice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Focused unit tests for {@link FileUploadController}'s cleanup safety guard.
 *
 * <p>The load-bearing piece of the "delete previous avatar" logic is the URL
 * validation: we must only delete files that look like /uploads/avatars/&lt;uuid&gt;.&lt;ext&gt;,
 * never arbitrary paths. That logic is extracted into {@code extractAvatarFilename}
 * so it can be tested without a Spring context or filesystem.
 *
 * <p>Filesystem effect (Files.deleteIfExists) is covered by the end-to-end
 * verification in the plan; it's plumbing, not security-critical logic.
 */
class FileUploadControllerTest {

    private static final String PREFIX = "/uploads/avatars/";

    @Nested
    @DisplayName("extractAvatarFilename — path traversal guard")
    class ExtractAvatarFilename {

        @ParameterizedTest(name = "[{index}] accept {0}")
        @ValueSource(strings = {
            "/uploads/avatars/18c522c2-ff2b-46a7-b012-f124bb66f068.jpg",
            "/uploads/avatars/92731650-9f6b-41a6-966b-277484fc9461.png",
            "/uploads/avatars/abc.gif",
            "/uploads/avatars/my-avatar.webp"
        })
        @DisplayName("accepts well-formed avatar URLs and returns the bare filename")
        void acceptsWellFormed(String url) {
            String expected = url.substring(PREFIX.length());
            assertEquals(expected, FileUploadController.extractAvatarFilename(url));
        }

        @ParameterizedTest(name = "[{index}] reject {0}")
        @ValueSource(strings = {
            "/uploads/avatars/../../etc/passwd",
            "/uploads/avatars/..%2F..%2Fetc%2Fpasswd",
            "/uploads/avatars/foo.html",
            "/uploads/avatars/no-extension",
            "/uploads/avatars/foo.JPG",
            "/uploads/avatars/foo.png.bak",
            "/uploads/avatars/",
            "/uploads/images/abc.png",
            "/uploads/avatars",
            "/api/upload/avatar",
            "http://evil.example/uploads/avatars/x.png"
        })
        @DisplayName("rejects traversal attempts and non-conformant paths")
        void rejectsUnsafe(String url) {
            assertNull(FileUploadController.extractAvatarFilename(url), "should reject: " + url);
        }

        @ParameterizedTest(name = "[{index}] null/blank → null")
        @NullAndEmptySource
        @DisplayName("returns null for null/empty input")
        void returnsNullForBlank(String url) {
            assertNull(FileUploadController.extractAvatarFilename(url));
        }
    }
}
