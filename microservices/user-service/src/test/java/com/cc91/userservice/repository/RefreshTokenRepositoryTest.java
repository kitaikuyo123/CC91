package com.cc91.userservice.repository;

import com.cc91.userservice.entity.RefreshToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshToken persist(Long userId, String token, boolean revoked) {
        RefreshToken t = new RefreshToken(userId, token, LocalDateTime.now().plusDays(1));
        t.setRevoked(revoked);
        return refreshTokenRepository.save(t);
    }

    @Test
    @DisplayName("findByToken should return token when present")
    void findByTokenReturns() {
        persist(1L, "tok-1", false);
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("tok-1");
        assertTrue(found.isPresent());
        assertEquals(1L, found.get().getUserId());
    }

    @Test
    @DisplayName("findByToken should return empty when missing")
    void findByTokenMissing() {
        assertTrue(refreshTokenRepository.findByToken("ghost").isEmpty());
    }

    @Test
    @DisplayName("findByUserIdAndRevokedFalse should exclude revoked tokens")
    void findByUserIdAndRevokedFalse() {
        persist(7L, "a", false);
        persist(7L, "b", false);
        persist(7L, "c", true);
        persist(8L, "d", false);

        List<RefreshToken> active = refreshTokenRepository.findByUserIdAndRevokedFalse(7L);
        assertEquals(2, active.size());
        assertTrue(active.stream().allMatch(t -> !t.getRevoked()));
        assertTrue(active.stream().allMatch(t -> t.getUserId() == 7L));
    }

    @Test
    @DisplayName("deleteByUserId should remove only that user's tokens")
    void deleteByUserId() {
        persist(7L, "a", false);
        persist(7L, "b", true);
        persist(8L, "c", false);

        refreshTokenRepository.deleteByUserId(7L);
        refreshTokenRepository.flush();

        assertEquals(0, refreshTokenRepository.findByUserIdAndRevokedFalse(7L).size());
        assertTrue(refreshTokenRepository.findByToken("a").isEmpty());
        assertTrue(refreshTokenRepository.findByToken("b").isEmpty());
        // other user untouched
        assertTrue(refreshTokenRepository.findByToken("c").isPresent());
    }
}
