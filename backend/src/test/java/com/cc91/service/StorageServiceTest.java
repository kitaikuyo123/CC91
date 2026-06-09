package com.cc91.service;

import com.cc91.exception.BadRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StorageService business logic tests
 * Covers file validation (type/size/extension), path traversal protection, and delete operations
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.upload.avatar-dir=target/test-avatars",
    "app.upload.max-size=2097152"
})
class StorageServiceTest {

    @Autowired
    private StorageService storageService;

    private static final Path AVATAR_DIR = Path.of("target/test-avatars");

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(AVATAR_DIR);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(AVATAR_DIR)) {
            Files.walk(AVATAR_DIR)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                    });
        }
    }

    // ==================== store ====================

    @Test
    void store_ValidJpeg_ReturnsUrl() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", "test-content".getBytes()
        );

        String url = storageService.store(file, 1L);

        assertNotNull(url);
        assertTrue(url.startsWith("/uploads/avatars/"));
        assertTrue(url.endsWith(".jpg"));
        assertTrue(url.contains("1_"));
    }

    @Test
    void store_ValidPng_ReturnsUrl() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", "png-content".getBytes()
        );

        String url = storageService.store(file, 2L);

        assertNotNull(url);
        assertTrue(url.endsWith(".png"));
    }

    @Test
    void store_ValidWebp_ReturnsUrl() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "icon.webp", "image/webp", "webp-content".getBytes()
        );

        String url = storageService.store(file, 3L);

        assertNotNull(url);
        assertTrue(url.endsWith(".webp"));
    }

    @Test
    void store_UppercaseExtension_ReturnsNormalizedUrl() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.JPG", "image/jpeg", "content".getBytes()
        );

        String url = storageService.store(file, 4L);

        assertNotNull(url);
        assertTrue(url.endsWith(".jpg")); // extension normalized to lowercase
    }

    @Test
    void store_EmptyFile_ThrowsBadRequestException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", new byte[0]
        );

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> storageService.store(file, 1L));
        assertEquals("上传文件不能为空", ex.getMessage());
    }

    @Test
    void store_NullContentType_ThrowsBadRequestException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", null, "content".getBytes()
        );

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> storageService.store(file, 1L));
        assertEquals("仅支持 JPG、PNG、WebP 格式的图片", ex.getMessage());
    }

    @Test
    void store_UnsupportedContentType_ThrowsBadRequestException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.gif", "image/gif", "gif-content".getBytes()
        );

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> storageService.store(file, 1L));
        assertEquals("仅支持 JPG、PNG、WebP 格式的图片", ex.getMessage());
    }

    @Test
    void store_FileTooLarge_ThrowsBadRequestException() {
        // Temporarily set max-size to 10 bytes to test the size check
        ReflectionTestUtils.setField(storageService, "maxFileSize", 10L);
        try {
            byte[] largeContent = new byte[100];
            MockMultipartFile file = new MockMultipartFile(
                    "file", "avatar.jpg", "image/jpeg", largeContent
            );

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> storageService.store(file, 1L));
            assertEquals("文件大小不能超过 2MB", ex.getMessage());
        } finally {
            // Restore default max size
            ReflectionTestUtils.setField(storageService, "maxFileSize", 2097152L);
        }
    }

    @Test
    void store_UnsupportedExtension_ThrowsBadRequestException() {
        // MIME type valid (image/jpeg) but extension not in allowlist (.bmp)
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.bmp", "image/jpeg", "content".getBytes()
        );

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> storageService.store(file, 1L));
        assertEquals("不支持的文件扩展名", ex.getMessage());
    }

    @Test
    void store_NoExtension_ThrowsBadRequestException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "noextension", "image/jpeg", "content".getBytes()
        );

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> storageService.store(file, 1L));
        assertEquals("不支持的文件扩展名", ex.getMessage());
    }

    // ==================== delete ====================

    @Test
    void delete_ValidUrl_RemovesFile() throws IOException {
        // Store a file first
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", "content".getBytes()
        );
        String url = storageService.store(file, 10L);

        // Verify file exists before delete
        String filename = url.substring("/uploads/avatars/".length());
        assertTrue(Files.exists(AVATAR_DIR.resolve(filename)));

        // Act
        storageService.delete(url);

        // Assert: file removed
        assertFalse(Files.exists(AVATAR_DIR.resolve(filename)));
    }

    @Test
    void delete_NullUrl_NoOp() {
        assertDoesNotThrow(() -> storageService.delete(null));
    }

    @Test
    void delete_NonAvatarPath_NoOp() {
        assertDoesNotThrow(() -> storageService.delete("/other/path/file.jpg"));
    }

    @Test
    void delete_PathTraversalDoubleDot_Rejected() {
        // Should not throw, just log warning
        assertDoesNotThrow(() ->
                storageService.delete("/uploads/avatars/../../etc/passwd"));
    }

    @Test
    void delete_PathTraversalSlashInFilename_Rejected() {
        assertDoesNotThrow(() ->
                storageService.delete("/uploads/avatars/subdir/file.jpg"));
    }

    @Test
    void delete_NonExistentFile_NoOp() {
        assertDoesNotThrow(() ->
                storageService.delete("/uploads/avatars/nonexistent_999.jpg"));
    }
}
