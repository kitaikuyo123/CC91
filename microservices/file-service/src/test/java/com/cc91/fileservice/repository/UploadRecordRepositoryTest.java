package com.cc91.fileservice.repository;

import com.cc91.fileservice.entity.UploadRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link UploadRecordRepository} @DataJpaTest.
 *
 * @DataJpaTest swaps the production MySQL DataSource for an embedded H2 and
 * sets ddl-auto=create-drop. The @TestPropertySource override is required
 * because {@code db/migration/V1__create_upload_record.sql} is MySQL syntax
 * that does not parse under H2 MODE=MySQL ("INDEX idx_..." is treated as a
 * data type) — so Flyway MUST be disabled here, letting Hibernate DDL be the
 * sole schema source.
 *
 * Verifies the basic JPA contract (save / findById / findAll) and that the
 * entity's @PrePersist callback populates createdAt.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class UploadRecordRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UploadRecordRepository repository;

    private UploadRecord buildRecord(String filename, String username) {
        UploadRecord r = new UploadRecord();
        r.setUploaderUserId(1L);
        r.setUploaderUsername(username);
        r.setFilename(filename);
        r.setOriginalFilename("orig-" + filename);
        r.setContentType("image/png");
        r.setFileSize(1024L);
        r.setUrl("/uploads/avatars/" + filename);
        r.setPurpose("AVATAR");
        return r;
    }

    @Nested
    @DisplayName("save + findById")
    class SaveAndFindById {

        @Test
        @DisplayName("should assign an auto-generated id and persist all fields")
        void shouldPersistAndAssignId() {
            UploadRecord saved = repository.save(buildRecord("abc-uuid.png", "alice"));

            assertNotNull(saved.getId());
            Optional<UploadRecord> fetched = repository.findById(saved.getId());
            assertTrue(fetched.isPresent());
            assertEquals("abc-uuid.png", fetched.get().getFilename());
            assertEquals("alice", fetched.get().getUploaderUsername());
            assertEquals("AVATAR", fetched.get().getPurpose());
            assertEquals(1024L, fetched.get().getFileSize());
        }

        @Test
        @DisplayName("should populate createdAt via @PrePersist on flush")
        void shouldPopulateCreatedAt() {
            UploadRecord saved = repository.save(buildRecord("ts-uuid.png", "alice"));
            entityManager.flush();
            entityManager.refresh(saved);

            assertNotNull(saved.getCreatedAt());
            // createdAt should be within a 5s window of "now"
            assertTrue(saved.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(5)));
        }

        @Test
        @DisplayName("should preserve nullable uploaderUserId=null")
        void shouldPersistNullUserId() {
            UploadRecord r = buildRecord("anon-uuid.png", "anon");
            r.setUploaderUserId(null);
            UploadRecord saved = repository.save(r);

            Optional<UploadRecord> fetched = repository.findById(saved.getId());
            assertTrue(fetched.isPresent());
            assertNull(fetched.get().getUploaderUserId());
            assertEquals("anon", fetched.get().getUploaderUsername());
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return all persisted records in insertion order")
        void shouldReturnAllRecords() {
            repository.save(buildRecord("u1.png", "alice"));
            repository.save(buildRecord("u2.png", "bob"));
            repository.save(buildRecord("u3.png", "carol"));
            entityManager.flush();

            List<UploadRecord> all = repository.findAll();
            assertEquals(3, all.size());
        }

        @Test
        @DisplayName("should return empty when no records")
        void shouldReturnEmptyWhenNoRecords() {
            List<UploadRecord> all = repository.findAll();
            assertNotNull(all);
            assertTrue(all.isEmpty());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should remove a record by id")
        void shouldDeleteRecord() {
            UploadRecord saved = repository.save(buildRecord("del-uuid.png", "alice"));
            entityManager.flush();

            repository.deleteById(saved.getId());
            entityManager.flush();

            assertFalse(repository.findById(saved.getId()).isPresent());
        }
    }
}
