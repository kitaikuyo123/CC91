package com.cc91.contentservice.repository;

import com.cc91.contentservice.entity.Report;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReportRepository @DataJpaTest against H2.
 *
 * ActiveProfiles("test") pulls in src/test/resources/application-test.yml which
 * disables Flyway and switches to H2. The content-service main application.yml
 * explicitly enables Flyway, so without this annotation the slice test would
 * attempt to run Flyway migrations (and fail on duplicate V1 migrations).
 */
@DataJpaTest
@ActiveProfiles("test")
class ReportRepositoryTest {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Report persist(Long reporterId, Report.TargetType type, Long targetId,
                           String reason, Report.ReportStatus status,
                           LocalDateTime createdAt) {
        Report r = new Report(reporterId, type, targetId, reason);
        r.setStatus(status);
        r.setCreatedAt(createdAt);
        entityManager.persist(r);
        entityManager.flush();
        return r;
    }

    @Nested
    @DisplayName("findByStatusOrderByCreatedAtDesc")
    class FindByStatus {

        @Test
        @DisplayName("should return only reports matching status, ordered by createdAt DESC")
        void shouldFilterByStatusAndSortDesc() {
            persist(1L, Report.TargetType.POST, 10L, "spam",
                    Report.ReportStatus.PENDING, LocalDateTime.of(2024, 1, 1, 10, 0));
            persist(1L, Report.TargetType.POST, 11L, "abuse",
                    Report.ReportStatus.RESOLVED, LocalDateTime.of(2024, 1, 2, 10, 0));
            Report laterPending = persist(1L, Report.TargetType.POST, 12L, "spam2",
                    Report.ReportStatus.PENDING, LocalDateTime.of(2024, 1, 3, 10, 0));

            Page<Report> result = reportRepository.findByStatusOrderByCreatedAtDesc(
                    Report.ReportStatus.PENDING, PageRequest.of(0, 10));

            assertEquals(2, result.getTotalElements());
            // newer (2024-01-03) comes first
            assertEquals(laterPending.getId(), result.getContent().get(0).getId());
            result.getContent().forEach(r ->
                    assertEquals(Report.ReportStatus.PENDING, r.getStatus()));
        }

        @Test
        @DisplayName("should return empty page when no reports match status")
        void shouldReturnEmptyWhenNoMatch() {
            persist(1L, Report.TargetType.POST, 10L, "spam",
                    Report.ReportStatus.PENDING, LocalDateTime.now());

            Page<Report> result = reportRepository.findByStatusOrderByCreatedAtDesc(
                    Report.ReportStatus.RESOLVED, PageRequest.of(0, 10));
            assertEquals(0, result.getTotalElements());
            assertTrue(result.getContent().isEmpty());
        }

        @Test
        @DisplayName("should paginate results")
        void shouldPaginate() {
            for (int i = 0; i < 5; i++) {
                persist(1L, Report.TargetType.POST, (long) i, "spam",
                        Report.ReportStatus.PENDING,
                        LocalDateTime.of(2024, 1, 1, 10, i));
            }
            Page<Report> page1 = reportRepository.findByStatusOrderByCreatedAtDesc(
                    Report.ReportStatus.PENDING, PageRequest.of(0, 2));
            Page<Report> page2 = reportRepository.findByStatusOrderByCreatedAtDesc(
                    Report.ReportStatus.PENDING, PageRequest.of(1, 2));

            assertEquals(5, page1.getTotalElements());
            assertEquals(2, page1.getNumberOfElements());
            assertEquals(2, page2.getNumberOfElements());
        }
    }

    @Nested
    @DisplayName("findAllByOrderByCreatedAtDesc")
    class FindAll {

        @Test
        @DisplayName("should return all reports ordered by createdAt DESC")
        void shouldReturnAllOrderedByCreatedAtDesc() {
            Report oldest = persist(1L, Report.TargetType.POST, 10L, "spam",
                    Report.ReportStatus.PENDING, LocalDateTime.of(2024, 1, 1, 10, 0));
            Report newest = persist(1L, Report.TargetType.POST, 11L, "abuse",
                    Report.ReportStatus.RESOLVED, LocalDateTime.of(2024, 1, 5, 10, 0));

            Page<Report> result = reportRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10));

            assertEquals(2, result.getTotalElements());
            assertEquals(newest.getId(), result.getContent().get(0).getId());
            assertEquals(oldest.getId(), result.getContent().get(1).getId());
        }

        @Test
        @DisplayName("should return empty page when no reports exist")
        void shouldReturnEmptyWhenNone() {
            Page<Report> result = reportRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10));
            assertEquals(0, result.getTotalElements());
        }
    }

    @Test
    @DisplayName("should persist report with default status PENDING via entity default")
    void shouldDefaultToPending() {
        Report r = new Report(1L, Report.TargetType.POST, 10L, "spam");
        entityManager.persist(r);
        entityManager.flush();
        // default @Column-defined status should be PENDING
        assertEquals(Report.ReportStatus.PENDING, r.getStatus());
    }
}
