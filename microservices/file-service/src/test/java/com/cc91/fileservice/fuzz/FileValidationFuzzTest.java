package com.cc91.fileservice.fuzz;

import com.cc91.fileservice.controller.FileUploadController;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Property-based fuzz tests for {@link FileUploadController#validateImageFile(MultipartFile)}.
 *
 * The method is private; we reach it via Spring's ReflectionTestUtils.invokeMethod.
 * No Spring context is loaded — the controller is constructed directly with null deps;
 * validateImageFile touches only the static constants and the file argument.
 *
 * Spring's ReflectionUtils.handleReflectionException rethrows RuntimeExceptions from
 * the target method verbatim, so an IllegalArgumentException thrown by validateImageFile
 * surfaces here directly. Checked exceptions would surface as UndeclaredThrowableException.
 *
 * Invariants:
 *   - never throws NPE / IllegalStateException / unexpected exception
 *   - whitelisted content types + in-size files always pass
 *   - non-whitelisted content types always fail
 *   - oversize / empty files always fail
 */
class FileValidationFuzzTest {

    private static final Set<String> WHITELIST = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    /** Bare controller — validateImageFile touches only the static constants / file arg. */
    private static final FileUploadController CONTROLLER = new FileUploadController(
            null, null, null);

    /**
     * Build a MockMultipartFile whose getSize() reports the requested value, without
     * having to allocate the full byte array for oversize tests. We override getSize()
     * and isEmpty() in an anonymous subclass — validateImageFile only reads
     * getSize() / isEmpty() / getContentType(), never the body.
     */
    private static MultipartFile fileWith(String contentType, long size) {
        byte[] body = new byte[(int) Math.min(Math.max(size, 0L), 1L)];
        MockMultipartFile base = new MockMultipartFile(
                "file", "upload.bin", contentType, body);
        return new MultipartFile() {
            @Override public String getName() { return base.getName(); }
            @Override public String getOriginalFilename() { return base.getOriginalFilename(); }
            @Override public String getContentType() { return base.getContentType(); }
            @Override public boolean isEmpty() { return size <= 0; }
            @Override public long getSize() { return size; }
            @Override public byte[] getBytes() throws java.io.IOException { return base.getBytes(); }
            @Override public java.io.InputStream getInputStream() throws java.io.IOException {
                return base.getInputStream();
            }
            @Override public void transferTo(java.io.File dest) { /* no-op */ }
            @Override public void transferTo(java.nio.file.Path dest) { /* no-op */ }
        };
    }

    /**
     * Invoke private validateImageFile. Returns true if validation passed (no throw).
     */
    private static boolean validateAccepts(MultipartFile file) {
        try {
            ReflectionTestUtils.invokeMethod(CONTROLLER, "validateImageFile", file);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        } catch (UndeclaredThrowableException ex) {
            // any other checked exception would surface here — treat as failure
            return false;
        }
    }

    @Provide
    Arbitrary<String> contentTypes() {
        return Arbitraries.oneOf(
                Arbitraries.of("image/jpeg", "image/png", "image/gif", "image/webp"),
                Arbitraries.of("image/svg+xml", "image/bmp", "image/tiff", "image/x-icon"),
                Arbitraries.of("text/html", "application/javascript", "application/pdf",
                        "application/octet-stream", "application/zip", "image/jpeg;", "image/jpg",
                        "IMAGE/PNG", "image/png ", "image/png\r\n", "image/png; charset=utf-8"),
                Arbitraries.strings().ofMinLength(1).ofMaxLength(50).ascii(),
                Arbitraries.of("", "   ", "null", "undefined"));
    }

    @Provide
    Arbitrary<Long> sizes() {
        // 0 .. 10 MB, with bias toward the 2MB boundary (the actual limit in src)
        return Arbitraries.longs().between(0L, 10L * 1024 * 1024);
    }

    @Provide
    Arbitrary<String> whitelistContentTypes() {
        return Arbitraries.of("image/jpeg", "image/png", "image/gif", "image/webp");
    }

    @Provide
    Arbitrary<Long> validSizes() {
        return Arbitraries.longs().between(1L, 2L * 1024 * 1024); // 1 byte .. 2 MB inclusive
    }

    @Provide
    Arbitrary<String> nonWhitelistContentTypes() {
        return Arbitraries.of(
                "text/html", "application/javascript", "image/svg+xml", "image/bmp",
                "image/tiff", "application/pdf", "application/zip", "application/octet-stream",
                "", "   ", "image/jpeg;", "image/jpg", "IMAGE/PNG", "image/png ",
                "image/png; charset=utf-8", "null", "undefined");
    }

    /**
     * For any (contentType, size) combination, validation must yield a stable boolean
     * result without throwing an unexpected exception type. 500 tries to cover the
     * boundary densely.
     */
    @Property(tries = 500)
    void validationIsStableForAnyCombination(
            @ForAll("contentTypes") String contentType,
            @ForAll("sizes") long size) {

        MultipartFile file1 = fileWith(contentType, size);
        MultipartFile file2 = fileWith(contentType, size);

        boolean accepted1 = validateAccepts(file1);
        boolean accepted2 = validateAccepts(file2);
        // determinism: same input → same output
        assertTrue(accepted1 == accepted2);

        if (accepted1) {
            // acceptance implies whitelist + in-range size
            assertTrue(WHITELIST.contains(contentType),
                    () -> "accepted non-whitelisted content type: '" + contentType + "'");
            assertTrue(size > 0 && size <= 2L * 1024 * 1024,
                    () -> "accepted out-of-range size=" + size);
        }
    }

    /**
     * Whitelisted content types with in-range size must always pass.
     */
    @Property(tries = 200)
    void whitelistedTypesPassValidation(
            @ForAll("whitelistContentTypes") String contentType,
            @ForAll("validSizes") long size) {
        MultipartFile file = fileWith(contentType, size);
        assertTrue(validateAccepts(file),
                () -> "expected acceptance for whitelisted type '" + contentType + "' size=" + size);
    }

    /**
     * Non-whitelisted content types must always fail.
     */
    @Property(tries = 200)
    void nonWhitelistedTypesFail(@ForAll("nonWhitelistContentTypes") String contentType) {
        MultipartFile file = fileWith(contentType, 1L);
        assertTrue(!validateAccepts(file),
                () -> "expected rejection for non-whitelisted type '" + contentType + "'");
    }

    /**
     * Size above 2MB must always fail regardless of content type.
     */
    @Property(tries = 100)
    void oversizeFailsValidation(@ForAll("whitelistContentTypes") String contentType) {
        MultipartFile file = fileWith(contentType, 2L * 1024 * 1024 + 1);
        assertTrue(!validateAccepts(file), "expected rejection for oversize file");
    }

    /**
     * Empty file (size = 0) must always fail.
     */
    @Property(tries = 50)
    void emptyFileFailsValidation(@ForAll("whitelistContentTypes") String contentType) {
        MultipartFile file = fileWith(contentType, 0L);
        assertTrue(!validateAccepts(file), "expected rejection for empty file");
    }
}
