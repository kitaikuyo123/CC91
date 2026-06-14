package com.cc91.contentservice.repository;

import com.cc91.contentservice.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 公告数据访问层
 * Microservice version: removed @Query JOIN FETCH methods.
 * Uses Spring Data findAll(Sort) instead of custom JPQL with author join.
 */
@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    // findAllWithAuthor() replaced by findAll(Sort.by(DESC,"isPinned").and(Sort.by(DESC,"createdAt")))
    // findByIdWithAuthor() replaced by findById()
}
