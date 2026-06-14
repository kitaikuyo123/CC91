package com.cc91.forumservice.repository;

import com.cc91.forumservice.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByUserIdAndPostId(Long userId, Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PostLike pl WHERE pl.userId = :userId AND pl.postId = :postId")
    void deleteByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    long countByPostId(Long postId);
}
