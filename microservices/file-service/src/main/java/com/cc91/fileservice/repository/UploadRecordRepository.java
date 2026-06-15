package com.cc91.fileservice.repository;

import com.cc91.fileservice.entity.UploadRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link UploadRecord}.
 * Intentionally empty — extend with query methods when audit / quota
 * features are needed.
 */
public interface UploadRecordRepository extends JpaRepository<UploadRecord, Long> {
}
