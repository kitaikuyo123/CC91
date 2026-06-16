package com.cc91.userservice.repository;

import com.cc91.userservice.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserRepository slice tests against H2.
 */
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User persist(String username, String email) {
        User u = new User(username, email, "hash");
        return userRepository.save(u);
    }

    @Test
    @DisplayName("findByUsername should return user when present")
    void findByUsernameReturns() {
        persist("alice", "a@b.com");
        Optional<User> found = userRepository.findByUsername("alice");
        assertTrue(found.isPresent());
        assertEquals("a@b.com", found.get().getEmail());
    }

    @Test
    @DisplayName("findByUsername should return empty when missing")
    void findByUsernameEmpty() {
        assertTrue(userRepository.findByUsername("ghost").isEmpty());
    }

    @Test
    @DisplayName("findByEmail should return user when present")
    void findByEmailReturns() {
        persist("alice", "a@b.com");
        assertTrue(userRepository.findByEmail("a@b.com").isPresent());
    }

    @Test
    @DisplayName("existsByUsername should reflect presence")
    void existsByUsername() {
        persist("alice", "a@b.com");
        assertTrue(userRepository.existsByUsername("alice"));
        assertFalse(userRepository.existsByUsername("bob"));
    }

    @Test
    @DisplayName("existsByEmail should reflect presence")
    void existsByEmail() {
        persist("alice", "a@b.com");
        assertTrue(userRepository.existsByEmail("a@b.com"));
        assertFalse(userRepository.existsByEmail("ghost@example.com"));
    }

    @Test
    @DisplayName("default role should be USER")
    void defaultRoleIsUser() {
        User u = persist("alice", "a@b.com");
        assertEquals("USER", u.getRole());
    }

    @Test
    @DisplayName("default isLocked should be false")
    void defaultLockedIsFalse() {
        User u = persist("alice", "a@b.com");
        assertFalse(u.getIsLocked());
    }
}
