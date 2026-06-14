package com.cc91.fileservice.controller;

import com.cc91.fileservice.client.UserServiceClient;
import com.cc91.fileservice.dto.ApiResponse;
import com.cc91.fileservice.dto.UpdateAvatarRequest;
import com.cc91.fileservice.dto.UserInfoDTO;
import com.cc91.fileservice.exception.UnauthorizedException;
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

    public FileUploadController(UserServiceClient userServiceClient, JwtUtil jwtUtil) {
        this.userServiceClient = userServiceClient;
        this.jwtUtil = jwtUtil;
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
            try {
                userServiceClient.updateAvatar(userId, new UpdateAvatarRequest(avatarUrl));
                logger.info("Avatar updated for user {} (userId={})", username, userId);
            } catch (Exception ex) {
                logger.error("Failed to update avatar in User Service for userId={}: {}", userId, ex.getMessage());
                // File is saved, but User Service update failed
                // Return success with a warning
                return ResponseEntity.ok(ApiResponse.success("Avatar uploaded but profile update may be delayed",
                        Map.of("avatarUrl", avatarUrl)));
            }
        }

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
        getCurrentUsername(); // Verify authenticated

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
     * Generates a unique filename preserving the original extension.
     */
    private String generateFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".png";
        return UUID.randomUUID().toString() + ext;
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
