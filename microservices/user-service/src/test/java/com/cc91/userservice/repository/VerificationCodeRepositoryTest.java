package com.cc91.userservice.repository;

import com.cc91.userservice.entity.VerificationCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class VerificationCodeRepositoryTest {

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    private VerificationCode persist(String email, String code, String type) {
        VerificationCode v = new VerificationCode(email, code, type,
                LocalDateTime.now().plusMinutes(10));
        return verificationCodeRepository.save(v);
    }

    @Test
    @DisplayName("findByEmailAndCode should resolve by (email, code) regardless of type")
    void findByEmailAndCode() {
        persist("a@b.com", "123456", "REGISTER");
        Optional<VerificationCode> found =
                verificationCodeRepository.findByEmailAndCode("a@b.com", "123456");
        assertTrue(found.isPresent());
        assertEquals("REGISTER", found.get().getType());
    }

    @Test
    @DisplayName("findByEmailAndCodeAndType should filter by type")
    void findByEmailAndCodeAndTypeFiltersType() {
        persist("a@b.com", "123456", "REGISTER");
        persist("a@b.com", "654321", "PASSWORD_RESET");

        assertTrue(verificationCodeRepository
                .findByEmailAndCodeAndType("a@b.com", "123456", "REGISTER").isPresent());
        // same code different type → should be empty
        assertTrue(verificationCodeRepository
                .findByEmailAndCodeAndType("a@b.com", "123456", "PASSWORD_RESET").isEmpty());
    }

    @Test
    @DisplayName("should return empty when no match")
    void shouldReturnEmptyWhenNoMatch() {
        assertTrue(verificationCodeRepository
                .findByEmailAndCode("ghost", "000000").isEmpty());
        assertTrue(verificationCodeRepository
                .findByEmailAndCodeAndType("ghost", "000000", "REGISTER").isEmpty());
    }

    @Test
    @DisplayName("findFirstByEmailAndTypeOrderByCreatedAtDesc should return latest")
    void findFirstByEmailAndTypeOrderedByCreatedAt() {
        // Insert in order — createdAt set on @PrePersist; rely on save timing.
        VerificationCode first = persist("a@b.com", "111111", "REGISTER");
        VerificationCode second = persist("a@b.com", "222222", "REGISTER");

        Optional<VerificationCode> latest = verificationCodeRepository
                .findFirstByEmailAndTypeOrderByCreatedAtDesc("a@b.com", "REGISTER");
        assertTrue(latest.isPresent());
        // The latest is whichever was persisted last (second). IDs auto-increment.
        assertTrue(latest.get().getId() >= first.getId());
        assertTrue(latest.get().getId() >= second.getId());
    }
}
