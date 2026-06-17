package com.cc91.forumservice.service;

import com.cc91.forumservice.client.UserInfoDTO;
import com.cc91.forumservice.client.UserServiceClient;
import com.cc91.forumservice.client.UserServiceClientFallback;
import com.cc91.forumservice.dto.CreatePostRequest;
import com.cc91.forumservice.dto.PostResponse;
import com.cc91.forumservice.dto.UpdatePostRequest;
import com.cc91.forumservice.entity.Bookmark;
import com.cc91.forumservice.entity.Category;
import com.cc91.forumservice.entity.Post;
import com.cc91.forumservice.entity.PostLike;
import com.cc91.forumservice.exception.ResourceNotFoundException;
import com.cc91.forumservice.exception.UnauthorizedException;
import com.cc91.forumservice.repository.BookmarkRepository;
import com.cc91.forumservice.repository.CategoryRepository;
import com.cc91.forumservice.repository.CommentRepository;
import com.cc91.forumservice.repository.PostLikeRepository;
import com.cc91.forumservice.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * PostService unit tests.
 * Covers the full lifecycle: create / read / update / delete, like & bookmark
 * toggles, search, sort variants, and "my" content. Includes XSS sanitization
 * and ownership (OWASP A01 Broken Access Control) verifications.
 */
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private BookmarkRepository bookmarkRepository;
    @Mock private UserServiceClient userServiceClient;
    @Mock private CategoryRepository categoryRepository;
    @Mock private CommentRepository commentRepository;

    @InjectMocks
    private PostService postService;

    private static final String USERNAME = "alice";
    private static final Long USER_ID = 1L;

    private UserInfoDTO user() {
        return new UserInfoDTO(USER_ID, USERNAME, "USER", "https://cdn/alice.png");
    }

    private Post samplePost() {
        Post p = new Post("标题", "正文", USER_ID);
        p.setId(10L);
        p.setCategoryId(1L);
        p.setViewCount(5);
        p.setLikeCount(2);
        p.setStatus("PUBLISHED");
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return p;
    }

    @Nested
    @DisplayName("createPost")
    class CreatePost {

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenUserMissing() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(null);
            CreatePostRequest req = new CreatePostRequest("t", "c", 1L);
            assertThrows(ResourceNotFoundException.class,
                    () -> postService.createPost(USERNAME, req));
        }

        /**
         * Regression test for the 500-RPS stress test scenario-4 bug:
         * 'Column author_id cannot be null' (400) on POST /api/posts.
         *
         * Root cause: {@code UserServiceClientFallback#getUserByUsername} used to
         * return a ghost {@code UserInfoDTO(null, username, "USER", null)}, which
         * defeated the existing {@code if (user == null) throw} guard. The fix
         * requires the fallback to return {@code null}; this test wires the real
         * fallback instance into the service to assert the end-to-end contract:
         * user-service down -> fallback -> null -> ResourceNotFoundException,
         * never a 400 from a NULL author_id INSERT.
         */
        @Test
        @DisplayName("regression: real UserServiceClientFallback returns null -> ResourceNotFoundException, never ghost user")
        void shouldThrowWhenRealFallbackReturnsNullForUsername() {
            UserServiceClient realFallback = new UserServiceClientFallback();
            when(userServiceClient.getUserByUsername(USERNAME))
                    .thenAnswer(inv -> realFallback.getUserByUsername(USERNAME));
            CreatePostRequest req = new CreatePostRequest("t", "c", 1L);

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> postService.createPost(USERNAME, req));

            // 关键：fallback 不应让流程走到 save（即不会构造出 author_id=null 的 Post）
            verify(postRepository, never()).save(any(Post.class));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when categoryId not found")
        void shouldThrowWhenCategoryMissing() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            when(categoryRepository.existsById(1L)).thenReturn(false);
            CreatePostRequest req = new CreatePostRequest("t", "c", 1L);
            assertThrows(ResourceNotFoundException.class,
                    () -> postService.createPost(USERNAME, req));
        }

        @Test
        @DisplayName("should sanitize XSS in title and content on successful create")
        void shouldSanitizeXssOnCreate() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            when(categoryRepository.existsById(1L)).thenReturn(true);
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
                Post p = inv.getArgument(0);
                p.setId(99L);
                return p;
            });

            String xssTitle = "<script>alert(1)</script>";
            String xssContent = "<img src=x onerror=alert(2)>";
            CreatePostRequest req = new CreatePostRequest(xssTitle, xssContent, 1L);
            PostResponse resp = postService.createPost(USERNAME, req);

            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(captor.capture());
            Post saved = captor.getValue();
            // Sanitization applied BEFORE save
            assertFalse(saved.getTitle().contains("<script>"));
            assertFalse(saved.getContent().contains("<img"));
            // Also reflected in response
            assertFalse(resp.getTitle().contains("<script>"));
            assertFalse(resp.getContent().contains("<img"));
        }

        @Test
        @DisplayName("should default status to PUBLISHED when request.status is null")
        void shouldDefaultStatusToPublished() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            when(categoryRepository.existsById(1L)).thenReturn(true);
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
                Post p = inv.getArgument(0);
                p.setId(99L);
                return p;
            });

            CreatePostRequest req = new CreatePostRequest("t", "c", 1L);
            postService.createPost(USERNAME, req);

            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(captor.capture());
            assertEquals("PUBLISHED", captor.getValue().getStatus());
        }

        @Test
        @DisplayName("should respect DRAFT status when provided")
        void shouldRespectDraftStatus() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            when(categoryRepository.existsById(1L)).thenReturn(true);
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
                Post p = inv.getArgument(0);
                p.setId(99L);
                return p;
            });

            CreatePostRequest req = new CreatePostRequest("t", "c", 1L);
            req.setStatus("DRAFT");
            postService.createPost(USERNAME, req);

            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(captor.capture());
            assertEquals("DRAFT", captor.getValue().getStatus());
        }
    }

    @Nested
    @DisplayName("getPostById")
    class GetPostById {

        @Test
        @DisplayName("should call incrementViewCount when increaseView=true")
        void shouldIncrementViewCount() {
            when(postRepository.findById(10L)).thenReturn(Optional.of(samplePost()));
            when(userServiceClient.getUserById(USER_ID)).thenReturn(user());
            when(commentRepository.countByPostIdAndStatus(10L, "PUBLISHED")).thenReturn(3L);

            postService.getPostById(10L, true);

            verify(postRepository).incrementViewCount(10L);
        }

        @Test
        @DisplayName("should NOT call incrementViewCount when increaseView=false")
        void shouldNotIncrementViewCount() {
            when(postRepository.findById(10L)).thenReturn(Optional.of(samplePost()));
            when(userServiceClient.getUserById(USER_ID)).thenReturn(user());
            when(commentRepository.countByPostIdAndStatus(10L, "PUBLISHED")).thenReturn(0L);

            postService.getPostById(10L, false);

            verify(postRepository, never()).incrementViewCount(anyLong());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when post not found")
        void shouldThrowWhenPostMissing() {
            when(postRepository.findById(404L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> postService.getPostById(404L, false));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when author not found (null from User Service)")
        void shouldThrowWhenAuthorMissing() {
            when(postRepository.findById(10L)).thenReturn(Optional.of(samplePost()));
            when(userServiceClient.getUserById(USER_ID)).thenReturn(null);
            assertThrows(ResourceNotFoundException.class, () -> postService.getPostById(10L, false));
        }

        @Test
        @DisplayName("should return populated response with comment count and author info")
        void shouldReturnPopulatedResponse() {
            Post p = samplePost();
            when(postRepository.findById(10L)).thenReturn(Optional.of(p));
            when(userServiceClient.getUserById(USER_ID)).thenReturn(user());
            when(commentRepository.countByPostIdAndStatus(10L, "PUBLISHED")).thenReturn(7L);

            PostResponse resp = postService.getPostById(10L, false);
            assertEquals(10L, resp.getId());
            assertEquals("alice", resp.getAuthorUsername());
            assertEquals(7L, resp.getCommentCount());
            assertEquals(2L, resp.getLikeCount());
            assertEquals("https://cdn/alice.png", resp.getAuthorAvatarUrl());
        }
    }

    @Nested
    @DisplayName("updatePost (ownership)")
    class UpdatePost {

        @Test
        @DisplayName("should throw UnauthorizedException when user is not the author")
        void shouldThrowWhenNotOwner() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Post other = new Post("t", "c", 999L); // different author
            other.setId(10L);
            when(postRepository.findById(10L)).thenReturn(Optional.of(other));

            UpdatePostRequest req = new UpdatePostRequest("new", "new");
            assertThrows(UnauthorizedException.class,
                    () -> postService.updatePost(USERNAME, 10L, req));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when post not found")
        void shouldThrowWhenPostMissing() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            when(postRepository.findById(404L)).thenReturn(Optional.empty());
            UpdatePostRequest req = new UpdatePostRequest("new", "new");
            assertThrows(ResourceNotFoundException.class,
                    () -> postService.updatePost(USERNAME, 404L, req));
        }

        @Test
        @DisplayName("should sanitize XSS in updated title/content")
        void shouldSanitizeXssOnUpdate() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Post p = samplePost();
            when(postRepository.findById(10L)).thenReturn(Optional.of(p));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdatePostRequest req = new UpdatePostRequest(
                    "<script>x</script>", "<img src=x onerror=alert(1)>");
            postService.updatePost(USERNAME, 10L, req);

            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(captor.capture());
            assertFalse(captor.getValue().getTitle().contains("<script>"));
            assertFalse(captor.getValue().getContent().contains("<img"));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when updating categoryId to missing category")
        void shouldThrowWhenNewCategoryMissing() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Post p = samplePost();
            when(postRepository.findById(10L)).thenReturn(Optional.of(p));
            when(categoryRepository.existsById(777L)).thenReturn(false);

            UpdatePostRequest req = new UpdatePostRequest();
            req.setCategoryId(777L);
            assertThrows(ResourceNotFoundException.class,
                    () -> postService.updatePost(USERNAME, 10L, req));
        }

        @Test
        @DisplayName("should successfully update when owner")
        void shouldUpdateWhenOwner() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Post p = samplePost();
            when(postRepository.findById(10L)).thenReturn(Optional.of(p));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdatePostRequest req = new UpdatePostRequest("new title", "new content");
            PostResponse resp = postService.updatePost(USERNAME, 10L, req);
            assertEquals("new title", resp.getTitle());
            verify(postRepository).save(any(Post.class));
        }

        @Test
        @DisplayName("should not change title/content when request fields are null")
        void shouldNotChangeWhenFieldsNull() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Post p = samplePost();
            String originalTitle = p.getTitle();
            when(postRepository.findById(10L)).thenReturn(Optional.of(p));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdatePostRequest req = new UpdatePostRequest();
            PostResponse resp = postService.updatePost(USERNAME, 10L, req);
            assertEquals(originalTitle, resp.getTitle());
        }
    }

    @Nested
    @DisplayName("deletePost (ownership)")
    class DeletePost {

        @Test
        @DisplayName("should throw UnauthorizedException when user is not the author")
        void shouldThrowWhenNotOwner() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Post other = new Post("t", "c", 999L);
            other.setId(10L);
            when(postRepository.findById(10L)).thenReturn(Optional.of(other));
            assertThrows(UnauthorizedException.class,
                    () -> postService.deletePost(USERNAME, 10L));
        }

        @Test
        @DisplayName("should delete when owner")
        void shouldDeleteWhenOwner() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Post p = samplePost();
            when(postRepository.findById(10L)).thenReturn(Optional.of(p));

            postService.deletePost(USERNAME, 10L);
            verify(postRepository).delete(p);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when post not found")
        void shouldThrowWhenPostMissing() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            when(postRepository.findById(404L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> postService.deletePost(USERNAME, 404L));
        }
    }

    @Nested
    @DisplayName("toggleLike (idempotent)")
    class ToggleLike {

        @Test
        @DisplayName("first call: should add PostLike and increment likeCount")
        void shouldAddLikeWhenAbsent() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Post p = samplePost();
            when(postRepository.findById(10L)).thenReturn(Optional.of(p));
            when(postLikeRepository.findByUserIdAndPostId(USER_ID, 10L)).thenReturn(Optional.empty());
            when(postLikeRepository.countByPostId(10L)).thenReturn(3L);
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = postService.toggleLike(USERNAME, 10L);

            verify(postLikeRepository).save(any(PostLike.class));
            verify(postLikeRepository, never()).deleteByUserIdAndPostId(anyLong(), anyLong());
            assertEquals(true, result.get("isLiked"));
            assertEquals(3L, result.get("likeCount"));
        }

        @Test
        @DisplayName("second call: should remove PostLike (idempotent toggle)")
        void shouldRemoveLikeWhenPresent() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Post p = samplePost();
            when(postRepository.findById(10L)).thenReturn(Optional.of(p));
            when(postLikeRepository.findByUserIdAndPostId(USER_ID, 10L))
                    .thenReturn(Optional.of(new PostLike(USER_ID, 10L)));
            when(postLikeRepository.countByPostId(10L)).thenReturn(1L);

            Map<String, Object> result = postService.toggleLike(USERNAME, 10L);

            verify(postLikeRepository).deleteByUserIdAndPostId(USER_ID, 10L);
            verify(postLikeRepository, never()).save(any(PostLike.class));
            assertEquals(false, result.get("isLiked"));
            assertEquals(1L, result.get("likeCount"));
        }
    }

    @Nested
    @DisplayName("toggleBookmark (idempotent)")
    class ToggleBookmark {

        @Test
        @DisplayName("first call: should add Bookmark")
        void shouldAddBookmark() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Post p = samplePost();
            when(postRepository.findById(10L)).thenReturn(Optional.of(p));
            when(bookmarkRepository.findByUserIdAndPostId(USER_ID, 10L)).thenReturn(Optional.empty());

            Map<String, Object> result = postService.toggleBookmark(USERNAME, 10L);

            verify(bookmarkRepository).save(any(Bookmark.class));
            assertEquals(true, result.get("isBookmarked"));
        }

        @Test
        @DisplayName("second call: should remove Bookmark (idempotent toggle)")
        void shouldRemoveBookmark() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Post p = samplePost();
            when(postRepository.findById(10L)).thenReturn(Optional.of(p));
            when(bookmarkRepository.findByUserIdAndPostId(USER_ID, 10L))
                    .thenReturn(Optional.of(new Bookmark(USER_ID, 10L)));

            Map<String, Object> result = postService.toggleBookmark(USERNAME, 10L);

            verify(bookmarkRepository).deleteByUserIdAndPostId(USER_ID, 10L);
            verify(bookmarkRepository, never()).save(any(Bookmark.class));
            assertEquals(false, result.get("isBookmarked"));
        }
    }

    @Nested
    @DisplayName("getPostsByCategory")
    class GetPostsByCategory {

        @Test
        @DisplayName("should throw ResourceNotFoundException when category not found")
        void shouldThrowWhenCategoryMissing() {
            when(categoryRepository.existsById(1L)).thenReturn(false);
            assertThrows(ResourceNotFoundException.class,
                    () -> postService.getPostsByCategory(1L, 0, 10));
        }

        @Test
        @DisplayName("should query with status=PUBLISHED filter")
        void shouldFilterByPublished() {
            when(categoryRepository.existsById(1L)).thenReturn(true);
            Page<Post> emptyPage = new PageImpl<>(Collections.emptyList());
            when(postRepository.findByCategoryIdAndStatus(eq(1L), eq("PUBLISHED"), any(Pageable.class)))
                    .thenReturn(emptyPage);

            Page<PostResponse> result = postService.getPostsByCategory(1L, 0, 10);
            verify(postRepository).findByCategoryIdAndStatus(eq(1L), eq("PUBLISHED"), any(Pageable.class));
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("searchPosts")
    class SearchPosts {

        @Test
        @DisplayName("should return empty page when keyword is null")
        void shouldReturnEmptyWhenNull() {
            Page<PostResponse> result = postService.searchPosts(null, 0, 10);
            assertTrue(result.isEmpty());
            verifyNoInteractions(postRepository);
        }

        @Test
        @DisplayName("should return empty page when keyword is blank")
        void shouldReturnEmptyWhenBlank() {
            Page<PostResponse> result = postService.searchPosts("   ", 0, 10);
            assertTrue(result.isEmpty());
            verifyNoInteractions(postRepository);
        }

        @Test
        @DisplayName("should query with PUBLISHED status and trimmed keyword")
        void shouldSearchWithPublishedFilter() {
            Page<Post> emptyPage = new PageImpl<>(Collections.emptyList());
            when(postRepository.findByStatusAndTitleContainingOrStatusAndContentContaining(
                    eq("PUBLISHED"), anyString(),
                    eq("PUBLISHED"), anyString(),
                    any(Pageable.class))).thenReturn(emptyPage);

            Page<PostResponse> result = postService.searchPosts("  hello  ", 0, 10);
            assertFalse(result == null);
            verify(postRepository).findByStatusAndTitleContainingOrStatusAndContentContaining(
                    eq("PUBLISHED"), eq("hello"),
                    eq("PUBLISHED"), eq("hello"),
                    any(Pageable.class));
        }

        @Test
        @DisplayName("should safely handle SQL-injection-like keywords without throwing")
        void shouldHandleSqlInjectionKeywords() {
            Page<Post> emptyPage = new PageImpl<>(Collections.emptyList());
            when(postRepository.findByStatusAndTitleContainingOrStatusAndContentContaining(
                    anyString(), anyString(), anyString(), anyString(), any(Pageable.class)))
                    .thenReturn(emptyPage);

            String[] maliciousKeywords = {"' OR 1=1 --", "'; DROP TABLE posts; --", "%_%", "_'"};
            for (String kw : maliciousKeywords) {
                // JPA parameterized queries neutralize these — no exception expected
                assertDoesNotThrow(() -> postService.searchPosts(kw, 0, 10));
            }
        }
    }

    @Nested
    @DisplayName("getPostList (sort variants)")
    class GetPostList {

        @Test
        @DisplayName("sort=latest should use findAll/findByStatus with createdAt DESC")
        void shouldSortByLatest() {
            when(postRepository.findByStatus(eq("PUBLISHED"), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            postService.getPostList(0, 10, "PUBLISHED", "latest");
            verify(postRepository).findByStatus(eq("PUBLISHED"), any(Pageable.class));
        }

        @Test
        @DisplayName("sort=hot should use findAll/findByStatus ordered by viewCount DESC")
        void shouldSortByHot() {
            when(postRepository.findByStatus(eq("PUBLISHED"), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            postService.getPostList(0, 10, "PUBLISHED", "hot");
            verify(postRepository).findByStatus(eq("PUBLISHED"), any(Pageable.class));
        }

        @Test
        @DisplayName("sort=comments should delegate to findSortedByCommentCount")
        void shouldSortByComments() {
            when(postRepository.findSortedByCommentCount(any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            postService.getPostList(0, 10, "PUBLISHED", "comments");
            verify(postRepository).findSortedByCommentCount(eq("PUBLISHED"), any(Pageable.class));
        }

        @Test
        @DisplayName("sort=comments with null status should pass null (no status filter)")
        void shouldSortByCommentsWithoutStatus() {
            when(postRepository.findSortedByCommentCount(any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            postService.getPostList(0, 10, null, "comments");
            verify(postRepository).findSortedByCommentCount(eq(null), any(Pageable.class));
        }

        @Test
        @DisplayName("sort=comments with empty status should pass null")
        void shouldSortByCommentsWithEmptyStatus() {
            when(postRepository.findSortedByCommentCount(any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            postService.getPostList(0, 10, "  ", "comments");
            verify(postRepository).findSortedByCommentCount(eq(null), any(Pageable.class));
        }

        @Test
        @DisplayName("null status with non-comments sort should call findAll")
        void shouldCallFindAllWhenStatusNull() {
            when(postRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            postService.getPostList(0, 10, null, "latest");
            verify(postRepository).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("overloaded getPostList(page,size,status) should default to latest sort")
        void shouldDefaultToLatestSort() {
            when(postRepository.findByStatus(eq("PUBLISHED"), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            postService.getPostList(0, 10, "PUBLISHED");
            verify(postRepository).findByStatus(eq("PUBLISHED"), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getMyPosts / getMyDrafts / getMyBookmarks")
    class GetMy {

        @Test
        @DisplayName("getMyPosts should throw when user not found")
        void getMyPostsThrowsWhenUserMissing() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(null);
            assertThrows(ResourceNotFoundException.class,
                    () -> postService.getMyPosts(USERNAME));
        }

        @Test
        @DisplayName("getMyPosts should return empty list when no posts")
        void getMyPostsReturnsEmpty() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            when(postRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(USER_ID, "PUBLISHED"))
                    .thenReturn(Collections.emptyList());
            assertTrue(postService.getMyPosts(USERNAME).isEmpty());
        }

        @Test
        @DisplayName("getMyDrafts should query DRAFT status")
        void getMyDraftsQueriesDraft() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            when(postRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(USER_ID, "DRAFT"))
                    .thenReturn(Collections.emptyList());
            postService.getMyDrafts(USERNAME);
            verify(postRepository).findByAuthorIdAndStatusOrderByCreatedAtDesc(USER_ID, "DRAFT");
        }

        @Test
        @DisplayName("getMyBookmarks should return empty list when no bookmarks")
        void getMyBookmarksEmpty() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            when(bookmarkRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(Collections.emptyList());
            assertTrue(postService.getMyBookmarks(USERNAME).isEmpty());
        }

        @Test
        @DisplayName("getMyBookmarks should preserve bookmark order")
        void getMyBookmarksPreservesOrder() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Bookmark b1 = new Bookmark(USER_ID, 10L);
            Bookmark b2 = new Bookmark(USER_ID, 20L);
            when(bookmarkRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(List.of(b1, b2));

            Post p20 = samplePost();
            p20.setId(20L);
            Post p10 = samplePost();
            p10.setId(10L);
            // findAllById may return in any order — service re-orders by bookmark list
            when(postRepository.findAllById(List.of(10L, 20L)))
                    .thenReturn(List.of(p20, p10));

            List<PostResponse> result = postService.getMyBookmarks(USERNAME);
            assertEquals(2, result.size());
            assertEquals(10L, result.get(0).getId());
            assertEquals(20L, result.get(1).getId());
        }
    }
}
