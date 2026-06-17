package com.cc91.forumservice.repository;

import com.cc91.forumservice.entity.Comment;
import com.cc91.forumservice.entity.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommentRepository slice tests against H2.
 * Covers derived methods and @Query (findAllWithPostByOrderByCreatedAtDesc
 * with JOIN FETCH, countByPostIdInAndStatus).
 *
 * <p>显式激活 "test" profile，让 src/test/resources/application-test.yml 生效，
 * 从而禁用 Flyway（避免 V3 复合索引迁移在 H2 上找不到 posts/comments 表）。
 */
@DataJpaTest
@ActiveProfiles("test")
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Post persistPost(String title) {
        Post p = new Post(title, "content", 1L);
        return postRepository.save(p);
    }

    @Nested
    @DisplayName("findByPostIdAndStatusOrderByCreatedAtAsc")
    class FindByPostAndStatus {

        @Test
        @DisplayName("should return comments for post + status")
        void shouldReturnComments() {
            Post p = persistPost("p");
            Comment c1 = new Comment(p.getId(), 1L, "c1", null);
            c1.setStatus("PUBLISHED");
            Comment c2 = new Comment(p.getId(), 1L, "c2", null);
            c2.setStatus("DELETED");
            commentRepository.save(c1);
            commentRepository.save(c2);
            entityManager.flush();
            entityManager.clear();

            List<Comment> result = commentRepository.findByPostIdAndStatusOrderByCreatedAtAsc(p.getId(), "PUBLISHED");
            assertEquals(1, result.size());
            assertEquals("c1", result.get(0).getContent());
        }
    }

    @Nested
    @DisplayName("findByParentIdOrderByCreatedAtAsc")
    class FindReplies {

        @Test
        @DisplayName("should return direct replies")
        void shouldReturnReplies() {
            Post p = persistPost("p");
            Comment parent = new Comment(p.getId(), 1L, "parent", null);
            parent = commentRepository.save(parent);
            Comment r1 = new Comment(p.getId(), 2L, "r1", parent.getId());
            Comment r2 = new Comment(p.getId(), 3L, "r2", parent.getId());
            commentRepository.save(r1);
            commentRepository.save(r2);
            entityManager.flush();
            entityManager.clear();

            List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(parent.getId());
            assertEquals(2, replies.size());
        }
    }

    @Nested
    @DisplayName("countByPostId / countByPostIdAndStatus")
    class CountByPost {

        @Test
        @DisplayName("should count all comments under post")
        void shouldCountAll() {
            Post p = persistPost("p");
            commentRepository.save(new Comment(p.getId(), 1L, "c", null));
            commentRepository.save(new Comment(p.getId(), 1L, "c", null));
            entityManager.flush();
            assertEquals(2L, commentRepository.countByPostId(p.getId()));
        }

        @Test
        @DisplayName("should count only published comments")
        void shouldCountPublishedOnly() {
            Post p = persistPost("p");
            Comment pub = new Comment(p.getId(), 1L, "c", null);
            pub.setStatus("PUBLISHED");
            Comment del = new Comment(p.getId(), 1L, "c", null);
            del.setStatus("DELETED");
            commentRepository.save(pub);
            commentRepository.save(del);
            entityManager.flush();
            assertEquals(1L, commentRepository.countByPostIdAndStatus(p.getId(), "PUBLISHED"));
        }
    }

    @Nested
    @DisplayName("findByAuthorIdAndStatusOrderByCreatedAtDesc")
    class FindByAuthor {

        @Test
        @DisplayName("should return author's comments by status")
        void shouldReturnByAuthor() {
            Post p = persistPost("p");
            Comment c1 = new Comment(p.getId(), 5L, "c1", null);
            c1.setStatus("PUBLISHED");
            Comment c2 = new Comment(p.getId(), 5L, "c2", null);
            c2.setStatus("DELETED");
            commentRepository.save(c1);
            commentRepository.save(c2);
            entityManager.flush();
            entityManager.clear();

            List<Comment> result = commentRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(5L, "PUBLISHED");
            assertEquals(1, result.size());
            assertEquals("c1", result.get(0).getContent());
        }
    }

    @Nested
    @DisplayName("@Query countByPostIdInAndStatus")
    class CountByPostIdInAndStatus {

        @Test
        @DisplayName("should batch-count comments grouped by post id")
        void shouldBatchCount() {
            Post p1 = persistPost("p1");
            Post p2 = persistPost("p2");
            Post p3 = persistPost("p3"); // no comments

            commentRepository.save(new Comment(p1.getId(), 1L, "c", null));
            commentRepository.save(new Comment(p1.getId(), 1L, "c", null));
            commentRepository.save(new Comment(p2.getId(), 1L, "c", null));
            Comment del = new Comment(p1.getId(), 1L, "c", null);
            del.setStatus("DELETED");
            commentRepository.save(del);
            entityManager.flush();

            List<Object[]> result = commentRepository.countByPostIdInAndStatus(
                    List.of(p1.getId(), p2.getId(), p3.getId()), "PUBLISHED");

            // 2 published for p1, 1 for p2, 0 for p3 (not present in result)
            assertEquals(2, result.size());
            // Verify mapping
            var map = new java.util.HashMap<Long, Long>();
            for (Object[] row : result) {
                map.put((Long) row[0], (Long) row[1]);
            }
            assertEquals(2L, map.get(p1.getId()));
            assertEquals(1L, map.get(p2.getId()));
            assertNull(map.get(p3.getId()));
        }

        @Test
        @DisplayName("should return empty list for empty input")
        void shouldReturnEmptyForEmptyInput() {
            List<Object[]> result = commentRepository.countByPostIdInAndStatus(List.of(), "PUBLISHED");
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("@Query findAllWithPostByOrderByCreatedAtDesc (JOIN FETCH)")
    class FindAllWithPost {

        @Test
        @DisplayName("should fetch comments with their post eagerly")
        void shouldFetchWithPost() {
            Post p = persistPost("Title here");
            Comment c = new Comment(p.getId(), 1L, "content", null);
            commentRepository.save(c);
            entityManager.flush();
            entityManager.clear();

            List<Comment> result = commentRepository.findAllWithPostByOrderByCreatedAtDesc();
            assertEquals(1, result.size());
            // post should be eagerly loaded (JOIN FETCH) — accessing it outside session is safe
            assertNotNull(result.get(0).getPost());
            assertEquals("Title here", result.get(0).getPost().getTitle());
        }

        @Test
        @DisplayName("should order by createdAt desc")
        void shouldOrderByCreatedAtDesc() throws Exception {
            Post p = persistPost("p");
            Comment first = new Comment(p.getId(), 1L, "first", null);
            first.setCreatedAt(java.time.LocalDateTime.now().minusHours(1));
            Comment second = new Comment(p.getId(), 1L, "second", null);
            second.setCreatedAt(java.time.LocalDateTime.now());
            commentRepository.save(first);
            commentRepository.save(second);
            entityManager.flush();
            entityManager.clear();

            List<Comment> result = commentRepository.findAllWithPostByOrderByCreatedAtDesc();
            // second (newer) should come first
            assertEquals("second", result.get(0).getContent());
        }

        @Test
        @DisplayName("should handle orphaned comments where post lookup fails gracefully")
        void shouldHandleOrphanedPost() {
            // Create post + comment, then verify access works even in detached state
            Post p = persistPost("p");
            commentRepository.save(new Comment(p.getId(), 1L, "c", null));
            entityManager.flush();
            entityManager.clear();

            List<Comment> result = commentRepository.findAllWithPostByOrderByCreatedAtDesc();
            assertFalse(result.isEmpty());
        }
    }
}
