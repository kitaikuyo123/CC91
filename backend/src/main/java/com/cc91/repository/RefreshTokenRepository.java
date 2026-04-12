package com.cc91.repository;

import com.cc91.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 刷新令牌数据访问层
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 根据令牌字符串查找
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 根据用户ID查找所有有效的刷新令牌
     */
    List<RefreshToken> findByUserIdAndRevokedFalse(Long userId);

    /**
     * 撤销用户的所有令牌
     */
    void deleteByUserId(Long userId);
}
