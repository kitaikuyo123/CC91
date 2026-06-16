package com.cc91.fileservice.fuzz;

import com.cc91.fileservice.controller.FileUploadController;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Property-based fuzz tests for {@link FileUploadController#generateFilename(org.springframework.web.multipart.MultipartFile)}.
 *
 * Invariants:
 *   - extension always comes from the whitelist {.jpg, .png, .gif, .webp}
 *   - filename always matches <UUID>.<ext>
 *
 * Even when fed malicious originalFilenames (path-traversal, emoji, control chars),
 * the generator must NOT derive the extension from the client-supplied name —
 * it derives it from the (already-whitelisted) Content-Type.
 */
class FilenameGenerationFuzzTest {

    private static final Set<String> ALLOWED_EXTS = Set.of(".jpg", ".png", ".gif", ".webp");
    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|gif|webp)$");

    private static final FileUploadController CONTROLLER = new FileUploadController(
            null, null, null);

    @Provide
    Arbitrary<String> originalFilenames() {
        Arbitrary<String> pathTraversal = Arbitraries.of(
                "../../../etc/passwd", "../../etc/shadow", "..\\..\\windows\\system32",
                "/etc/passwd", "C:\\Windows\\System32\\evil.exe",
                "..%2f..%2f..%2f", "..%5c..%5c", "....//....//", "/dev/null");
        Arbitrary<String> weirdNames = Arbitraries.of(
                " .exe", ".htaccess", "  ", "config.json", "shell.php", "shell.phtml",
                "shell.php.jpg", "image.JPG", "image.PNG", "image.jpeg", "image.gif",
                "  file.png  ", "café.jpg", "🔑.png", "中文名.png", "name with spaces.png");
        Arbitrary<String> randomAscii = Arbitraries.strings()
                .ofMinLength(0).ofMaxLength(80)
                .withChars("<>\"'&;|*?\n\r\t\\/.abcdefghijklmnopqrstuvwxyz0123456789.");
        return Arbitraries.oneOf(pathTraversal, weirdNames, randomAscii);
    }

    @Provide
    Arbitrary<String> whitelistContentTypes() {
        return Arbitraries.of("image/jpeg", "image/png", "image/gif", "image/webp");
    }

    private String generateFilename(String contentType, String originalFilename) {
        MockMultipartFile file = new MockMultipartFile(
                "file", originalFilename, contentType, new byte[]{1});
        return (String) ReflectionTestUtils.invokeMethod(CONTROLLER, "generateFilename", file);
    }

    /**
     * For any (whitelisted contentType, malicious originalFilename) pair, the generated
     * filename's extension must be drawn from the whitelist. The originalFilename is
     * ignored for extension purposes — this is the path-traversal / polyglot guard.
     */
    @Property(tries = 300)
    void extensionAlwaysFromWhitelist(
            @ForAll("whitelistContentTypes") String contentType,
            @ForAll("originalFilenames") String originalFilename) {

        String filename = generateFilename(contentType, originalFilename);
        String ext = filename.substring(filename.lastIndexOf('.'));
        assertTrue(ALLOWED_EXTS.contains(ext),
                () -> "filename '" + filename + "' for original='" + originalFilename
                        + "' ct='" + contentType + "' has non-whitelisted ext '" + ext + "'");
    }

    /**
     * Filename must always match the <UUID>.<ext> shape — no path components, no
     * client-controlled prefix, no whitespace.
     */
    @Property(tries = 300)
    void filenameMatchesUuidExtPattern(
            @ForAll("whitelistContentTypes") String contentType,
            @ForAll("originalFilenames") String originalFilename) {

        String filename = generateFilename(contentType, originalFilename);
        assertTrue(UUID_PATTERN.matcher(filename).matches(),
                () -> "filename '" + filename + "' (ct='" + contentType + "', original='"
                        + originalFilename + "') does not match <UUID>.<ext>");
    }
}
