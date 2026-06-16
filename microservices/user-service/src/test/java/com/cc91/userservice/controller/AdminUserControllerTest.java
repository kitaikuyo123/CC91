package com.cc91.userservice.controller;

import com.cc91.userservice.base.BaseWebMvcTest;
import com.cc91.userservice.entity.User;
import com.cc91.userservice.entity.UserProfile;
import com.cc91.userservice.repository.RefreshTokenRepository;
import com.cc91.userservice.repository.UserProfileRepository;
import com.cc91.userservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminUserController @WebMvcTest.
 * Class-level @PreAuthorize("hasRole('ADMIN')") — non-admins must receive 403.
 */
@WebMvcTest(controllers = AdminUserController.class)
class AdminUserControllerTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockBean
    private UserProfileRepository userProfileRepository;

    private User user(long id, String name) {
        User u = new User(name, name + "@b.com", "hash");
        u.setId(id);
        u.setRole("USER");
        return u;
    }

    @Nested
    @DisplayName("Authorization (OWASP A01)")
    class Authorization {

        @Test
        @DisplayName("should return 401 when anonymous")
        void shouldReturn401WhenAnonymous() throws Exception {
            mockMvc.perform(get("/api/admin/users"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 403 when role is USER (not ADMIN)")
        @WithMockUser(username = "alice", roles = "USER")
        void shouldReturn403WhenUser() throws Exception {
            mockMvc.perform(get("/api/admin/users"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 200 when role is ADMIN")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldReturn200WhenAdmin() throws Exception {
            when(userRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                    .thenReturn(List.of(user(1L, "alice"), user(2L, "bob")));
            mockMvc.perform(get("/api/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].username").value("alice"))
                    .andExpect(jsonPath("$[1].username").value("bob"));
        }
    }

    @Nested
    @DisplayName("PUT /{id}/ban")
    class Ban {

        @Test
        @DisplayName("should ban other user when admin")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldBanOther() throws Exception {
            User target = user(5L, "alice");
            when(userRepository.findById(5L)).thenReturn(Optional.of(target));
            mockMvc.perform(put("/api/admin/users/5/ban"))
                    .andExpect(status().isOk());
            verify(userRepository).save(argThat(u -> u.getIsLocked() && u.getLockUntil() == null));
        }

        @Test
        @DisplayName("should refuse to ban self")
        @WithMockUser(username = "alice", roles = "ADMIN")
        void shouldRefuseBanSelf() throws Exception {
            User target = user(5L, "alice");
            when(userRepository.findById(5L)).thenReturn(Optional.of(target));
            mockMvc.perform(put("/api/admin/users/5/ban"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /{id}/unban")
    class Unban {

        @Test
        @DisplayName("should unban user (clear lock + attempts)")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldUnban() throws Exception {
            User target = user(5L, "alice");
            target.setIsLocked(true);
            target.setFailedLoginAttempts(5);
            when(userRepository.findById(5L)).thenReturn(Optional.of(target));
            mockMvc.perform(put("/api/admin/users/5/unban"))
                    .andExpect(status().isOk());
            verify(userRepository).save(argThat(u ->
                    !u.getIsLocked() && u.getFailedLoginAttempts() == 0 && u.getLockUntil() == null));
        }
    }

    @Nested
    @DisplayName("PUT /{id}/role")
    class Role {

        @Test
        @DisplayName("should update role when admin")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldUpdateRole() throws Exception {
            User target = user(5L, "alice");
            when(userRepository.findById(5L)).thenReturn(Optional.of(target));
            mockMvc.perform(put("/api/admin/users/5/role")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\":\"ADMIN\"}"))
                    .andExpect(status().isOk());
            verify(userRepository).save(argThat(u -> "ADMIN".equals(u.getRole())));
        }

        @Test
        @DisplayName("should return 400 when role is not USER/ADMIN (validation)")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldReturn400WhenRoleInvalid() throws Exception {
            mockMvc.perform(put("/api/admin/users/5/role")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\":\"SUPERUSER\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /{id}")
    class Delete {

        @Test
        @DisplayName("should delete user + cascade refresh tokens + profile")
        @WithMockUser(username = "root", roles = "ADMIN")
        void shouldCascadeDelete() throws Exception {
            User target = user(5L, "alice");
            when(userRepository.findById(5L)).thenReturn(Optional.of(target));
            when(userProfileRepository.findByUserId(5L))
                    .thenReturn(Optional.of(new UserProfile(5L)));
            mockMvc.perform(delete("/api/admin/users/5"))
                    .andExpect(status().isOk());
            verify(refreshTokenRepository).deleteByUserId(5L);
            verify(userProfileRepository).delete(any(UserProfile.class));
            verify(userRepository).delete(target);
        }

        @Test
        @DisplayName("should refuse to delete self")
        @WithMockUser(username = "alice", roles = "ADMIN")
        void shouldRefuseDeleteSelf() throws Exception {
            User target = user(5L, "alice");
            when(userRepository.findById(5L)).thenReturn(Optional.of(target));
            mockMvc.perform(delete("/api/admin/users/5"))
                    .andExpect(status().isBadRequest());
            verify(userRepository, never()).delete(any());
        }
    }
}
