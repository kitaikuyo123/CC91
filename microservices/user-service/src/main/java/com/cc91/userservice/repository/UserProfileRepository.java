package com.cc91.userservice.repository;

import com.cc91.userservice.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户资料数据访问层
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /**
     * 根据用户ID查找用户资料
     */
    Optional<UserProfile> findByUserId(Long userId);
}
