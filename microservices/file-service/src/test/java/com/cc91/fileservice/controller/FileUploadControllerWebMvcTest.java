package com.cc91.fileservice.controller;

import com.cc91.fileservice.base.BaseWebMvcTest;
import com.cc91.fileservice.client.UserServiceClient;
import com.cc91.fileservice.dto.UpdateAvatarRequest;
import com.cc91.fileservice.dto.UserInfoDTO;
import com.cc91.fileservice.repository.UploadRecordRepository;
import com.cc91.fileservice.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc slice test for {@link FileUploadController}.
 *
 * Mocks UserServiceClient (Feign), UploadRecordRepository, JwtUtil, and
 * UserDetailsServiceImpl (the last two inherited from {@link BaseWebMvcTest}).
 * Uses MockMultipartFile to drive the multipart endpoints.
 *
 * Coverage:
 * - uploadAvatar / uploadImage success path (file saved + record persisted)
 * - validation rejections (size / content-type / empty)
 * - UserServiceClient failure on avatar upload → still returns 200 (best-effort)
 * - UploadRecord persistence failure → still returns 200 (best-effort, swallowed)
 *
 * NOTE: The controller writes the multipart bytes to disk under
 * {user.dir}/uploads/. The slice test exercises that path; the on-disk file is
 * an acceptable side effect (cleaned by `mvn clean`).
 */
@WebMvcTest(controllers = FileUploadController.class)
class FileUploadControllerWebMvcTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private UploadRecordRepository uploadRecordRepository;

    // ---------- fixtures ----------

    private static final byte[] PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,  // PNG magic
            0x00, 0x01, 0x02, 0x03                                    // some payload
    };

    private MockMultipartFile pngFile(String name) {
        return new MockMultipartFile("file", name, "image/png", PNG_BYTES);
    }

    private MockMultipartFile oversizeFile() {
        byte[] big = new byte[(2 * 1024 * 1024) + 1]; // 2MB + 1
        return new MockMultipartFile("file", "big.png", "image/png", big);
    }

    /**
     * The controller reads the JWT from the raw Authorization header (via
     * RequestContextHolder) to extract the caller's userId, then calls
     * userServiceClient.updateAvatar(userId, ...). With @WithMockUser alone,
     * no Bearer header is added, so extractTokenFromContext() returns null and
     * the controller skips the user-service update entirely. We add a Bearer
     * header to every avatar upload test that exercises the success path.
     */
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            avatarRequest(MockMultipartFile file) {
        return multipart("/api/upload/avatar").file(file)
                .header("Authorization", "Bearer fake-but-non-null.jwt.token");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            imageRequest(MockMultipartFile file) {
        return multipart("/api/upload/images").file(file)
                .header("Authorization", "Bearer fake-but-non-null.jwt.token");
    }

    // ---------- uploadAvatar ----------

    @Nested
    @DisplayName("POST /api/upload/avatar")
    class UploadAvatar {

        @Test
        @DisplayName("should return 200 and call UserServiceClient.updateAvatar on success")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldSucceedForValidFile() throws Exception {
            when(userServiceClient.getUserByUsername("alice"))
                    .thenReturn(new UserInfoDTO(7L, "alice", "USER", null));
            when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(7L);

            mockMvc.perform(avatarRequest(pngFile("avatar.png")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.avatarUrl").exists());

            verify(userServiceClient).updateAvatar(eq(7L), any(UpdateAvatarRequest.class));
            verify(uploadRecordRepository).save(any());
        }

        @Test
        @DisplayName("should return 400 when file is empty")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn400ForEmptyFile() throws Exception {
            MockMultipartFile empty = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
            mockMvc.perform(avatarRequest(empty))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(userServiceClient);
            verify(uploadRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("should return 400 when Content-Type is not in allowlist (pdf)")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn400ForDisallowedContentType() throws Exception {
            MockMultipartFile pdf = new MockMultipartFile(
                    "file", "evil.pdf", "application/pdf", "%PDF-1.4 ...".getBytes());
            mockMvc.perform(avatarRequest(pdf))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(userServiceClient);
            verify(uploadRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("should return 400 when file size exceeds 2MB")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn400ForOversizeFile() throws Exception {
            mockMvc.perform(avatarRequest(oversizeFile()))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(userServiceClient);
            verify(uploadRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("should still return 200 (best-effort) when UserServiceClient.updateAvatar fails")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldStillReturn200WhenUserServiceUpdateFails() throws Exception {
            when(userServiceClient.getUserByUsername("alice"))
                    .thenReturn(new UserInfoDTO(7L, "alice", "USER", null));
            when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(7L);
            doThrow(new RuntimeException("user-service down"))
                    .when(userServiceClient).updateAvatar(eq(7L), any());

            mockMvc.perform(avatarRequest(pngFile("avatar.png")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.avatarUrl").exists());

            // recordUpload is still invoked on the failure path
            verify(uploadRecordRepository).save(any());
        }

        @Test
        @DisplayName("should still return 200 when UploadRecord persistence fails (best-effort, swallowed)")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldStillReturn200WhenPersistenceFails() throws Exception {
            when(userServiceClient.getUserByUsername("alice"))
                    .thenReturn(new UserInfoDTO(7L, "alice", "USER", null));
            when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(7L);
            doThrow(new RuntimeException("db down"))
                    .when(uploadRecordRepository).save(any());

            mockMvc.perform(avatarRequest(pngFile("avatar.png")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(userServiceClient).updateAvatar(eq(7L), any());
        }
    }

    // ---------- uploadImage ----------

    @Nested
    @DisplayName("POST /api/upload/images")
    class UploadImage {

        @Test
        @DisplayName("should return 200 and persist UploadRecord on success")
        @WithMockUser(username = "bob", roles = "USER")
        void shouldSucceedForValidFile() throws Exception {
            when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(42L);

            mockMvc.perform(imageRequest(pngFile("photo.png")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.url").exists());

            verify(uploadRecordRepository).save(any());
        }

        @Test
        @DisplayName("should return 400 when Content-Type is not in allowlist")
        @WithMockUser(username = "bob", roles = "USER")
        void shouldReturn400ForDisallowedContentType() throws Exception {
            MockMultipartFile html = new MockMultipartFile(
                    "file", "evil.html", "text/html", "<html></html>".getBytes());
            mockMvc.perform(imageRequest(html))
                    .andExpect(status().isBadRequest());
            verify(uploadRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("should return 400 when file is empty")
        @WithMockUser(username = "bob", roles = "USER")
        void shouldReturn400ForEmptyFile() throws Exception {
            MockMultipartFile empty = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
            mockMvc.perform(imageRequest(empty))
                    .andExpect(status().isBadRequest());
            verify(uploadRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("should return 400 when file size exceeds 2MB")
        @WithMockUser(username = "bob", roles = "USER")
        void shouldReturn400ForOversizeFile() throws Exception {
            mockMvc.perform(imageRequest(oversizeFile()))
                    .andExpect(status().isBadRequest());
            verify(uploadRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("should still return 200 when persistence fails (best-effort)")
        @WithMockUser(username = "bob", roles = "USER")
        void shouldStillReturn200WhenPersistenceFails() throws Exception {
            when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(42L);
            doThrow(new RuntimeException("db down")).when(uploadRecordRepository).save(any());

            mockMvc.perform(imageRequest(pngFile("photo.png")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
