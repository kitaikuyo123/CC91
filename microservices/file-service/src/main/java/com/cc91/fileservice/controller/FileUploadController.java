package com.cc91.fileservice.controller;

import com.cc91.fileservice.client.UserServiceClient;
import com.cc91.fileservice.dto.ApiResponse;
import com.cc91.fileservice.dto.UpdateAvatarRequest;
import com.cc91.fileservice.dto.UserInfoDTO;
import com.cc91.fileservice.entity.UploadRecord;
import com.cc91.fileservice.exception.UnauthorizedException;
import com.cc91.fileservice.repository.UploadRecordRepository;
import com.cc91.fileservice.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles file uploads for the CC91 forum system.
 * Avatar uploads are validated, stored, and the User Service is notified.
 * General image uploads are validated and stored locally.
 */
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final Path UPLOAD_ROOT = Path.of(System.getProperty("user.dir"), "uploads");

    private final UserServiceClient userServiceClient;
    private final JwtUtil jwtUtil;
    private final UploadRecordRepository uploadRecordRepository;

    public FileUploadController(UserServiceClient userServiceClient,
                                JwtUtil jwtUtil,
                                UploadRecordRepository uploadRecordRepository) {
        this.userServiceClient = userServiceClient;
        this.jwtUtil = jwtUtil;
        this.uploadRecordRepository = uploadRecordRepository;
    }

    /**
     * Upload avatar image.
     * POST /api/upload/avatar
     * Validates the image, stores it under uploads/avatars/, and calls
     * User Service via Feign to update the user's avatar URL.
     */
    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String username = getCurrentUsername();

        // Validate file
        validateImageFile(file);

        // Generate unique filename
        String filename = generateFilename(file);

        // Save to uploads/avatars/
        Path uploadDir = UPLOAD_ROOT.resolve("avatars");
        Files.createDirectories(uploadDir);
        Path filePath = uploadDir.resolve(filename);
        file.transferTo(filePath.toFile());

        String avatarUrl = "/uploads/avatars/" + filename;

        // Call User Service to update avatar URL
        Long userId = getCurrentUserId();
        if (userId != null) {
            // Fetch current avatarUrl BEFORE updateAvatar so we can clean up
            // the previous file once DB has been switched to the new URL.
            String previousAvatarUrl = fetchCurrentAvatarUrl(username);

            try {
                userServiceClient.updateAvatar(userId, new UpdateAvatarRequest(avatarUrl));
                logger.info("Avatar updated for user {} (userId={})", username, userId);
                // DB now points to the new URL — safe to delete the old file.
                // updateAvatar-failure path below intentionally skips this:
                // DB still references the old URL, deleting would orphan the user.
                deletePreviousAvatar(previousAvatarUrl, avatarUrl);
            } catch (Exception ex) {
                logger.error("Failed to update avatar in User Service for userId={}: {}", userId, ex.getMessage());
                // File is saved, but User Service update failed
                // Still record upload metadata, then return success with a warning
                recordUpload(file, filename, avatarUrl, "AVATAR", username, userId);
                return ResponseEntity.ok(ApiResponse.success("Avatar uploaded but profile update may be delayed",
                        Map.of("avatarUrl", avatarUrl)));
            }
        }

        recordUpload(file, filename, avatarUrl, "AVATAR", username, userId);
        return ResponseEntity.ok(ApiResponse.success("Avatar uploaded successfully",
                Map.of("avatarUrl", avatarUrl)));
    }

    /**
     * Upload general image.
     * POST /api/upload/images
     * Validates the image and stores it under uploads/images/.
     */
    @PostMapping("/images")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String username = getCurrentUsername(); // Verify authenticated
        Long userId = getCurrentUserId();

        // Validate file
        validateImageFile(file);

        // Generate unique filename
        String filename = generateFilename(file);

        // Save to uploads/images/
        Path uploadDir = UPLOAD_ROOT.resolve("images");
        Files.createDirectories(uploadDir);
        Path filePath = uploadDir.resolve(filename);
        file.transferTo(filePath.toFile());

        String url = "/uploads/images/" + filename;
        recordUpload(file, filename, url, "IMAGE", username, userId);
        return ResponseEntity.ok(ApiResponse.success("Upload successful", Map.of("url", url)));
    }

    /**
     * Validates that the uploaded file is an image within size limits.
     */
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only jpg/png/gif/webp images are supported");
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("File size cannot exceed 2MB");
        }
    }

    /**
     * Generates a unique filename derived from the validated Content-Type.
     * Security: do NOT trust the client-supplied originalFilename extension,
     * which could be forged (e.g. upload evil.html with Content-Type=image/png).
     * validateImageFile already restricted contentType to one of the four
     * allow-listed image MIME types, so we map that to the extension here.
     */
    private String generateFilename(MultipartFile file) {
        String contentType = file.getContentType();
        String ext = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".png"; // fallback; validateImageFile guarantees one of the four above
        };
        return UUID.randomUUID().toString() + ext;
    }

    /**
     * Persists a row to upload_record after a successful file save.
     * Wrap in try/catch: persistence failure MUST NOT break the upload
     * response — the file is already on disk. Audit logs are best-effort.
     */
    private void recordUpload(MultipartFile file,
                              String storedFilename,
                              String url,
                              String purpose,
                              String username,
                              Long userId) {
        try {
            UploadRecord record = new UploadRecord();
            record.setUploaderUserId(userId);
            record.setUploaderUsername(username);
            record.setFilename(storedFilename);
            record.setOriginalFilename(file.getOriginalFilename());
            record.setContentType(file.getContentType());
            record.setFileSize(file.getSize());
            record.setUrl(url);
            record.setPurpose(purpose);
            uploadRecordRepository.save(record);
        } catch (Exception ex) {
            logger.warn("Failed to persist upload_record (purpose={}, filename={}, user={}): {}",
                    purpose, storedFilename, username, ex.getMessage());
        }
    }

    /**
     * Queries user-service for the caller's current avatarUrl.
     * Used after a successful avatar upload to decide whether to delete
     * the previous file. Any failure returns null (= nothing to clean),
     * never blocks the upload flow.
     */
    private String fetchCurrentAvatarUrl(String username) {
        try {
            UserInfoDTO userInfo = userServiceClient.getUserByUsername(username);
            return userInfo != null ? userInfo.getAvatarUrl() : null;
        } catch (Exception ex) {
            logger.warn("Could not fetch current avatar URL for cleanup (user={}): {}",
                    username, ex.getMessage());
            return null;
        }
    }

    /**
     * Best-effort deletion of the user's previous avatar file.
     * Only /uploads/avatars/<filename> URLs are accepted, and the filename
     * must match UUID + whitelisted image extension — guards against path
     * traversal (e.g. "../../etc/passwd") and against deleting arbitrary files.
     * Equal to newUrl → skip (don't delete the just-uploaded file).
     * Any exception is swallowed; cleanup is never load-bearing for the upload.
     */
    private void deletePreviousAvatar(String previousUrl, String newUrl) {
        if (previousUrl == null || previousUrl.equals(newUrl)) return;

        String filename = extractAvatarFilename(previousUrl);
        if (filename == null) {
            logger.warn("Skip avatar cleanup: URL not a safe avatar path : {}", previousUrl);
            return;
        }

        try {
            Path oldFile = UPLOAD_ROOT.resolve("avatars").resolve(filename);
            boolean deleted = Files.deleteIfExists(oldFile);
            if (deleted) {
                logger.info("Deleted previous avatar: {}", filename);
            }
        } catch (Exception ex) {
            logger.warn("Failed to delete previous avatar ({}): {}", previousUrl, ex.getMessage());
        }
    }

    /**
     * Returns the bare filename if {@code url} is a safe /uploads/avatars/
     * URL with a UUID-style + whitelisted image extension, otherwise null.
     * Extracted as a package-visible static so the path-traversal guard can
     * be unit-tested without spinning up the filesystem.
     */
    static String extractAvatarFilename(String url) {
        if (url == null) return null;
        String prefix = "/uploads/avatars/";
        if (!url.startsWith(prefix)) return null;
        String filename = url.substring(prefix.length());
        if (!filename.matches("[\\w-]+\\.(jpg|png|gif|webp)")) return null;
        return filename;
    }

    /**
     * Extracts the current authenticated username from Spring Security context.
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        throw new UnauthorizedException("User not authenticated");
    }

    /**
     * Extracts the current user ID from the JWT token in the request.
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            // Re-extract from the JWT using the security context token
            try {
                String token = extractTokenFromContext();
                if (token != null) {
                    return jwtUtil.getUserIdFromToken(token);
                }
            } catch (Exception ex) {
                logger.warn("Could not extract userId from token: {}", ex.getMessage());
            }
        }
        return null;
    }

    /**
     * Extracts the raw JWT from the current request's Authorization header.
     */
    private String extractTokenFromContext() {
        jakarta.servlet.http.HttpServletRequest request =
                ((org.springframework.web.context.request.ServletRequestAttributes)
                        org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                        .getRequest();
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
