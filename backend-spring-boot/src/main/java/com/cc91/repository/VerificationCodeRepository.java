package com.cc91.repository;

import com.cc91.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 验证码数据访问层
 */
@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    /**
     * 根据邮箱和验证码查找
     */
    Optional<VerificationCode> findByEmailAndCode(String email, String code);

    /**
     * 根据邮箱和类型查找最新的验证码
     */
    Optional<VerificationCode> findFirstByEmailAndTypeOrderByCreatedAtDesc(String email, String type);

    /**
     * 删除过期的验证码
     */
    void deleteByExpiresAtBefore(LocalDateTime expiresAt);

    /**
     * 查找指定邮箱的所有未使用且未过期的验证码
     */
    List<VerificationCode> findByEmailAndUsedAndExpiresAtAfter(
        String email,
        Boolean used,
        LocalDateTime expiresAt
    );
}
