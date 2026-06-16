package com.cc91.contentservice.service;

import com.cc91.contentservice.client.UserInfoDTO;
import com.cc91.contentservice.client.UserServiceClient;
import com.cc91.contentservice.entity.Report;
import com.cc91.contentservice.exception.BadRequestException;
import com.cc91.contentservice.exception.ResourceNotFoundException;
import com.cc91.contentservice.repository.ReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ReportService unit tests.
 * Covers user existence validation, description concatenation, status filtering
 * pagination, and the PENDING→REVIEWED/RESOLVED/DISMISSED state flow.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private ReportService service;

    private UserInfoDTO user(Long id, String name) {
        return new UserInfoDTO(id, name, "USER", null);
    }

    @Nested
    @DisplayName("createReport")
    class CreateReport {

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found (null)")
        void shouldThrowWhenUserNull() {
            when(userServiceClient.getUserByUsername("ghost")).thenReturn(null);
            assertThrows(ResourceNotFoundException.class, () ->
                    service.createReport("ghost", 1L, "POST", "spam", "desc"));
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user has null id")
        void shouldThrowWhenUserIdNull() {
            when(userServiceClient.getUserByUsername("ghost"))
                    .thenReturn(new UserInfoDTO(null, "ghost", "USER", null));
            assertThrows(ResourceNotFoundException.class, () ->
                    service.createReport("ghost", 1L, "POST", "spam", "desc"));
        }

        @Test
        @DisplayName("should append description to reason when description provided")
        void shouldAppendDescription() {
            when(userServiceClient.getUserByUsername("alice")).thenReturn(user(100L, "alice"));
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Report result = service.createReport("alice", 1L, "POST", "spam", "more details");

            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
            verify(reportRepository).save(captor.capture());
            assertEquals("spam | more details", captor.getValue().getReason());
            assertEquals(100L, captor.getValue().getReporterId());
            assertEquals(Report.TargetType.POST, captor.getValue().getTargetType());
            assertEquals(Report.ReportStatus.PENDING, captor.getValue().getStatus());
            assertEquals(result.getReason(), "spam | more details");
        }

        @Test
        @DisplayName("should keep only reason when description is blank")
        void shouldNotAppendBlankDescription() {
            when(userServiceClient.getUserByUsername("alice")).thenReturn(user(100L, "alice"));
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createReport("alice", 1L, "POST", "spam", "   ");

            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
            verify(reportRepository).save(captor.capture());
            assertEquals("spam", captor.getValue().getReason());
        }

        @Test
        @DisplayName("should keep only reason when description is null")
        void shouldNotAppendNullDescription() {
            when(userServiceClient.getUserByUsername("alice")).thenReturn(user(100L, "alice"));
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createReport("alice", 1L, "COMMENT", "abuse", null);

            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
            verify(reportRepository).save(captor.capture());
            assertEquals("abuse", captor.getValue().getReason());
            assertEquals(Report.TargetType.COMMENT, captor.getValue().getTargetType());
        }

        @Test
        @DisplayName("should accept case-insensitive target type (POST / post / Post)")
        void shouldAcceptCaseInsensitiveTargetType() {
            when(userServiceClient.getUserByUsername("alice")).thenReturn(user(100L, "alice"));
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createReport("alice", 1L, "post", "spam", null);
            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
            verify(reportRepository).save(captor.capture());
            assertEquals(Report.TargetType.POST, captor.getValue().getTargetType());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException on invalid target type")
        void shouldThrowOnInvalidTargetType() {
            when(userServiceClient.getUserByUsername("alice")).thenReturn(user(100L, "alice"));
            assertThrows(IllegalArgumentException.class, () ->
                    service.createReport("alice", 1L, "USER", "spam", null));
        }
    }

    @Nested
    @DisplayName("getReports")
    class GetReports {

        @Test
        @DisplayName("should query all when status is null")
        void shouldQueryAllWhenStatusNull() {
            Page<Report> page = new PageImpl<>(List.of());
            when(reportRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);

            service.getReports(null, 0, 20);

            verify(reportRepository).findAllByOrderByCreatedAtDesc(any(Pageable.class));
            verify(reportRepository, never()).findByStatusOrderByCreatedAtDesc(any(), any());
        }

        @Test
        @DisplayName("should query all when status is empty/blank")
        void shouldQueryAllWhenStatusEmpty() {
            Page<Report> page = new PageImpl<>(List.of());
            when(reportRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);

            service.getReports("   ", 0, 20);

            verify(reportRepository).findAllByOrderByCreatedAtDesc(any(Pageable.class));
        }

        @Test
        @DisplayName("should query by status (case-insensitive) when provided")
        void shouldQueryByStatus() {
            Page<Report> page = new PageImpl<>(List.of());
            when(reportRepository.findByStatusOrderByCreatedAtDesc(eq(Report.ReportStatus.PENDING), any(Pageable.class)))
                    .thenReturn(page);

            service.getReports("pending", 0, 20);

            verify(reportRepository).findByStatusOrderByCreatedAtDesc(eq(Report.ReportStatus.PENDING), any(Pageable.class));
        }

        @Test
        @DisplayName("should construct Pageable with createdAt DESC sort")
        void shouldConstructPageableWithSort() {
            Page<Report> page = new PageImpl<>(List.of());
            when(reportRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);

            service.getReports(null, 2, 50);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(reportRepository).findAllByOrderByCreatedAtDesc(captor.capture());
            Pageable pageable = captor.getValue();
            assertEquals(PageRequest.of(2, 50), PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
            assertTrue(pageable.getSort().getOrderFor("createdAt").isDescending());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException on invalid status value")
        void shouldThrowOnInvalidStatus() {
            assertThrows(IllegalArgumentException.class, () -> service.getReports("INVALID", 0, 20));
        }
    }

    @Nested
    @DisplayName("handleReport")
    class HandleReport {

        @Test
        @DisplayName("should throw ResourceNotFoundException when report does not exist")
        void shouldThrowWhenNotFound() {
            when(reportRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () ->
                    service.handleReport(999L, "RESOLVED", "ok"));
        }

        @Test
        @DisplayName("should throw BadRequestException on invalid status")
        void shouldThrowOnInvalidStatus() {
            Report r = new Report(100L, Report.TargetType.POST, 1L, "reason");
            when(reportRepository.findById(1L)).thenReturn(Optional.of(r));

            assertThrows(BadRequestException.class, () ->
                    service.handleReport(1L, "BOGUS", null));
        }

        @Test
        @DisplayName("should transition PENDING → RESOLVED with adminComment and reviewedAt")
        void shouldTransitionToResolved() {
            Report r = new Report(100L, Report.TargetType.POST, 1L, "reason");
            when(reportRepository.findById(1L)).thenReturn(Optional.of(r));
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Report result = service.handleReport(1L, "RESOLVED", "resolved by admin");

            assertEquals(Report.ReportStatus.RESOLVED, result.getStatus());
            assertEquals("resolved by admin", result.getAdminComment());
            assertNotNull(result.getReviewedAt());
        }

        @Test
        @DisplayName("should transition PENDING → REVIEWED with status (case-insensitive)")
        void shouldTransitionToReviewed() {
            Report r = new Report(100L, Report.TargetType.POST, 1L, "reason");
            when(reportRepository.findById(1L)).thenReturn(Optional.of(r));
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Report result = service.handleReport(1L, "reviewed", "under review");
            assertEquals(Report.ReportStatus.REVIEWED, result.getStatus());
        }

        @Test
        @DisplayName("should transition PENDING → DISMISSED")
        void shouldTransitionToDismissed() {
            Report r = new Report(100L, Report.TargetType.POST, 1L, "reason");
            when(reportRepository.findById(1L)).thenReturn(Optional.of(r));
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Report result = service.handleReport(1L, "DISMISSED", null);
            assertEquals(Report.ReportStatus.DISMISSED, result.getStatus());
            // adminComment is null when not provided (not over-written)
            assertNull(result.getAdminComment());
            assertNotNull(result.getReviewedAt());
        }

        @Test
        @DisplayName("should preserve existing adminComment when null passed")
        void shouldNotOverwriteExistingComment() {
            Report r = new Report(100L, Report.TargetType.POST, 1L, "reason");
            r.setAdminComment("pre-existing");
            when(reportRepository.findById(1L)).thenReturn(Optional.of(r));
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Report result = service.handleReport(1L, "RESOLVED", null);
            assertEquals("pre-existing", result.getAdminComment());
        }
    }
}
