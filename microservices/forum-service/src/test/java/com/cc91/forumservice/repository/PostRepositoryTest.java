package com.cc91.forumservice.repository;

import com.cc91.forumservice.entity.Category;
import com.cc91.forumservice.entity.Comment;
import com.cc91.forumservice.entity.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostRepository slice tests against H2.
 * Covers derived query methods AND @Query custom queries (findSortedByCommentCount,
 * incrementViewCount, countStatsByCategory, search).
 */
@DataJpaTest
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TestEntityManager entityManager;

    /**
     * Posts have an FK constraint from category_id to categories(id).
     * Persist a Category (assigned an IDENTITY-generated id) and return it so
     * callers can use the generated id when constructing Posts.
     */
    private Category persistCategoryFresh() {
        Category c = new Category("Cat-" + System.nanoTime(), "desc", 0);
        entityManager.persist(c);
        entityManager.flush();
        return c;
    }

    private Post persist(String title, Long authorId, String status, Long categoryId) {
        Post p = new Post(title, "content-" + title, authorId);
        p.setStatus(status);
        p.setCategoryId(categoryId);
        return postRepository.save(p);
    }

    private Post persistWithCreatedAt(String title, Long authorId, String status, Long categoryId, LocalDateTime createdAt) {
        Post p = new Post(title, "content-" + title, authorId);
        p.setStatus(status);
        p.setCategoryId(categoryId);
        p.setCreatedAt(createdAt);
        p.setUpdatedAt(createdAt);
        return postRepository.save(p);
    }

    @Nested
    @DisplayName("findByAuthorIdAndStatusOrderByCreatedAtDesc")
    class FindByAuthorAndStatus {

        @Test
        @DisplayName("should return posts by author and status ordered by createdAt desc")
        void shouldFilter() {
            Post a = persist("a", 1L, "PUBLISHED", null);
            Post b = persist("b", 1L, "DRAFT", null);
            Post c = persist("c", 1L, "PUBLISHED", null);
            entityManager.flush();
            entityManager.clear();

            List<Post> published = postRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(1L, "PUBLISHED");
            assertEquals(2, published.size());
            assertTrue(published.stream().allMatch(p -> "PUBLISHED".equals(p.getStatus())));
            // DESC order: c was saved last, so it should appear first
            assertEquals("c", published.get(0).getTitle());
        }

        @Test
        @DisplayName("should not return other authors' posts")
        void shouldFilterByAuthor() {
            persist("a", 1L, "PUBLISHED", null);
            persist("b", 2L, "PUBLISHED", null);
            entityManager.flush();
            entityManager.clear();

            List<Post> result = postRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(2L, "PUBLISHED");
            assertEquals(1, result.size());
            assertEquals(2L, result.get(0).getAuthorId());
        }
    }

    @Nested
    @DisplayName("findByCategoryIdAndStatus")
    class FindByCategoryAndStatus {

        @Test
        @DisplayName("should filter by category and status")
        void shouldFilter() {
            Category c10 = persistCategoryFresh();
            Category c20 = persistCategoryFresh();
            persist("a", 1L, "PUBLISHED", c10.getId());
            persist("b", 1L, "PUBLISHED", c20.getId());
            persist("c", 1L, "DRAFT", c10.getId());
            entityManager.flush();
            entityManager.clear();

            Page<Post> result = postRepository.findByCategoryIdAndStatus(c10.getId(), "PUBLISHED", PageRequest.of(0, 10));
            assertEquals(1, result.getTotalElements());
            assertEquals(c10.getId(), result.getContent().get(0).getCategoryId());
        }
    }

    @Nested
    @DisplayName("countByCategoryId / countByCategoryIdAndStatus")
    class CountByCategory {

        @Test
        @DisplayName("should count all posts in category regardless of status")
        void shouldCountAll() {
            Category c = persistCategoryFresh();
            persist("a", 1L, "PUBLISHED", c.getId());
            persist("b", 1L, "DRAFT", c.getId());
            entityManager.flush();
            assertEquals(2L, postRepository.countByCategoryId(c.getId()));
        }

        @Test
        @DisplayName("should count published posts only")
        void shouldCountPublishedOnly() {
            Category c = persistCategoryFresh();
            persist("a", 1L, "PUBLISHED", c.getId());
            persist("b", 1L, "DRAFT", c.getId());
            entityManager.flush();
            assertEquals(1L, postRepository.countByCategoryIdAndStatus(c.getId(), "PUBLISHED"));
        }
    }

    @Nested
    @DisplayName("countByCategoryIdAndStatusAndCreatedAtAfter")
    class CountTodayPosts {

        @Test
        @DisplayName("should count posts created today")
        void shouldCountToday() {
            Category c = persistCategoryFresh();
            persistWithCreatedAt("a", 1L, "PUBLISHED", c.getId(), LocalDateTime.now());
            persistWithCreatedAt("old", 1L, "PUBLISHED", c.getId(), LocalDateTime.now().minusDays(2));
            entityManager.flush();
            entityManager.clear();

            long count = postRepository.countByCategoryIdAndStatusAndCreatedAtAfter(
                    c.getId(), "PUBLISHED", LocalDateTime.now().toLocalDate().atStartOfDay());
            assertEquals(1L, count);
        }
    }

    @Nested
    @DisplayName("@Query countStatsByCategory")
    class CountStatsByCategory {

        @Test
        @DisplayName("should group counts by category, filtering by status")
        void shouldGroupByCategory() {
            Category c10 = persistCategoryFresh();
            Category c20 = persistCategoryFresh();
            persist("a", 1L, "PUBLISHED", c10.getId());
            persist("b", 1L, "PUBLISHED", c10.getId());
            persist("c", 1L, "PUBLISHED", c20.getId());
            persist("d", 1L, "DRAFT", c10.getId()); // excluded by status filter
            entityManager.flush();

            List<Object[]> result = postRepository.countStatsByCategory(
                    "PUBLISHED", LocalDateTime.now().toLocalDate().atStartOfDay());
            assertFalse(result.isEmpty());
            // Should have rows for both categories
            assertTrue(result.stream().anyMatch(r -> c10.getId().equals(r[0])));
            assertTrue(result.stream().anyMatch(r -> c20.getId().equals(r[0])));
            // c10 has 2 published
            Object[] row10 = result.stream().filter(r -> c10.getId().equals(r[0])).findFirst().orElse(null);
            assertNotNull(row10);
            assertEquals(2L, ((Number) row10[1]).longValue());
        }

        @Test
        @DisplayName("todayPostCount should reflect posts created today")
        void shouldCountTodayPosts() {
            Category c = persistCategoryFresh();
            persistWithCreatedAt("today", 1L, "PUBLISHED", c.getId(), LocalDateTime.now());
            persistWithCreatedAt("yesterday", 1L, "PUBLISHED", c.getId(), LocalDateTime.now().minusDays(1));
            entityManager.flush();
            entityManager.clear();

            List<Object[]> result = postRepository.countStatsByCategory(
                    "PUBLISHED", LocalDateTime.now().toLocalDate().atStartOfDay());
            Object[] row10 = result.stream().filter(r -> c.getId().equals(r[0])).findFirst().orElse(null);
            assertNotNull(row10);
            // total published = 2, today = 1
            assertEquals(2L, ((Number) row10[1]).longValue());
            assertEquals(1L, ((Number) row10[2]).longValue());
        }
    }

    @Nested
    @DisplayName("findByStatusAndTitleContainingOrStatusAndContentContaining (search)")
    class Search {

        @Test
        @DisplayName("should match by title")
        void shouldMatchByTitle() {
            persist("Java tips", 1L, "PUBLISHED", null);
            persist("Other post", 1L, "PUBLISHED", null);
            entityManager.flush();
            entityManager.clear();

            Page<Post> result = postRepository.findByStatusAndTitleContainingOrStatusAndContentContaining(
                    "PUBLISHED", "Java",
                    "PUBLISHED", "Java",
                    PageRequest.of(0, 10));
            assertEquals(1, result.getTotalElements());
            assertEquals("Java tips", result.getContent().get(0).getTitle());
        }

        @Test
        @DisplayName("should match by content")
        void shouldMatchByContent() {
            Post p1 = persist("a", 1L, "PUBLISHED", null);
            p1.setContent("Learn Java here");
            postRepository.save(p1);
            persist("b", 1L, "PUBLISHED", null);
            entityManager.flush();
            entityManager.clear();

            Page<Post> result = postRepository.findByStatusAndTitleContainingOrStatusAndContentContaining(
                    "PUBLISHED", "Java",
                    "PUBLISHED", "Java",
                    PageRequest.of(0, 10));
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("should exclude non-PUBLISHED posts")
        void shouldExcludeDraft() {
            persist("Java tips", 1L, "DRAFT", null);
            entityManager.flush();

            Page<Post> result = postRepository.findByStatusAndTitleContainingOrStatusAndContentContaining(
                    "PUBLISHED", "Java",
                    "PUBLISHED", "Java",
                    PageRequest.of(0, 10));
            assertEquals(0, result.getTotalElements());
        }

        @Test
        @DisplayName("should be safe with SQL-injection-like keywords")
        void shouldBeSafeWithSqlInjectionKeywords() {
            persist("' OR 1=1 --", 1L, "PUBLISHED", null);
            entityManager.flush();
            // Should match the literal string, not inject SQL
            Page<Post> result = postRepository.findByStatusAndTitleContainingOrStatusAndContentContaining(
                    "PUBLISHED", "' OR 1=1 --",
                    "PUBLISHED", "' OR 1=1 --",
                    PageRequest.of(0, 10));
            // exact match only, no leak of other rows
            assertEquals(1, result.getTotalElements());
        }
    }

    @Nested
    @DisplayName("@Query findSortedByCommentCount")
    class FindSortedByCommentCount {

        @Test
        @DisplayName("should order posts by comment count desc when status null")
        void shouldSortByCommentCount() {
            Post p1 = persist("few", 1L, "PUBLISHED", null);
            Post p2 = persist("many", 1L, "PUBLISHED", null);
            entityManager.flush();
            // Create 1 comment on p1, 3 on p2
            for (int i = 0; i < 1; i++) {
                commentRepository.save(new Comment(p1.getId(), 1L, "c", null));
            }
            for (int i = 0; i < 3; i++) {
                commentRepository.save(new Comment(p2.getId(), 1L, "c", null));
            }
            entityManager.flush();
            entityManager.clear();

            Page<Post> result = postRepository.findSortedByCommentCount(null, PageRequest.of(0, 10));
            // p2 (3 comments) should come before p1 (1 comment)
            assertEquals("many", result.getContent().get(0).getTitle());
            assertEquals("few", result.getContent().get(1).getTitle());
        }

        @Test
        @DisplayName("should filter by status when provided")
        void shouldFilterByStatus() {
            Post pub = persist("pub", 1L, "PUBLISHED", null);
            Post draft = persist("draft", 1L, "DRAFT", null);
            entityManager.flush();
            commentRepository.save(new Comment(draft.getId(), 1L, "c", null));
            entityManager.flush();
            entityManager.clear();

            Page<Post> result = postRepository.findSortedByCommentCount("PUBLISHED", PageRequest.of(0, 10));
            // only the published post should be returned
            assertEquals(1, result.getTotalElements());
            assertEquals("pub", result.getContent().get(0).getTitle());
        }
    }

    @Nested
    @DisplayName("@Modifying incrementViewCount")
    class IncrementViewCount {

        @Test
        @DisplayName("should atomically increment view_count by 1")
        void shouldIncrementByOne() {
            Post p = persist("p", 1L, "PUBLISHED", null);
            entityManager.flush();
            int originalView = p.getViewCount();

            postRepository.incrementViewCount(p.getId());
            entityManager.flush();
            entityManager.clear();

            Post reloaded = postRepository.findById(p.getId()).orElseThrow();
            assertEquals(originalView + 1, reloaded.getViewCount());
        }

        @Test
        @DisplayName("should be idempotent-safe under multiple calls")
        void shouldHandleMultipleCalls() {
            Post p = persist("p", 1L, "PUBLISHED", null);
            entityManager.flush();
            int original = p.getViewCount();

            postRepository.incrementViewCount(p.getId());
            postRepository.incrementViewCount(p.getId());
            postRepository.incrementViewCount(p.getId());
            entityManager.flush();
            entityManager.clear();

            Post reloaded = postRepository.findById(p.getId()).orElseThrow();
            assertEquals(original + 3, reloaded.getViewCount());
        }

        @Test
        @DisplayName("should not affect other posts")
        void shouldNotAffectOthers() {
            Post p1 = persist("p1", 1L, "PUBLISHED", null);
            Post p2 = persist("p2", 1L, "PUBLISHED", null);
            entityManager.flush();

            postRepository.incrementViewCount(p1.getId());
            entityManager.flush();
            entityManager.clear();

            Post r1 = postRepository.findById(p1.getId()).orElseThrow();
            Post r2 = postRepository.findById(p2.getId()).orElseThrow();
            assertEquals(1, r1.getViewCount());
            assertEquals(0, r2.getViewCount());
        }
    }

    @Nested
    @DisplayName("findByStatus")
    class FindByStatus {

        @Test
        @DisplayName("should return only posts matching status")
        void shouldFilterByStatus() {
            persist("p1", 1L, "PUBLISHED", null);
            persist("p2", 1L, "DRAFT", null);
            entityManager.flush();

            Page<Post> published = postRepository.findByStatus("PUBLISHED", PageRequest.of(0, 10));
            assertEquals(1, published.getTotalElements());
        }
    }
}
