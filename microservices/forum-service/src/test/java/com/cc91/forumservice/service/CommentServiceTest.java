package com.cc91.forumservice.service;

import com.cc91.forumservice.client.CreateNotificationRequest;
import com.cc91.forumservice.client.NotificationServiceClient;
import com.cc91.forumservice.client.UserInfoDTO;
import com.cc91.forumservice.client.UserServiceClient;
import com.cc91.forumservice.dto.CommentResponse;
import com.cc91.forumservice.dto.CreateCommentRequest;
import com.cc91.forumservice.dto.UserCommentResponse;
import com.cc91.forumservice.entity.Comment;
import com.cc91.forumservice.entity.Post;
import com.cc91.forumservice.exception.ResourceNotFoundException;
import com.cc91.forumservice.exception.UnauthorizedException;
import com.cc91.forumservice.repository.CommentRepository;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CommentService unit tests.
 * Covers create / reply / update / delete (soft delete + cascade) and tree
 * building. Includes ownership verification (OWASP A01) and notification
 * behaviour.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private UserServiceClient userServiceClient;
    @Mock private NotificationServiceClient notificationServiceClient;

    @InjectMocks
    private CommentService commentService;

    private static final String USERNAME = "alice";
    private static final Long USER_ID = 1L;

    private UserInfoDTO user() {
        return new UserInfoDTO(USER_ID, USERNAME, "USER", "https://cdn/alice.png");
    }

    private Post samplePost() {
        Post p = new Post("标题", "正文", USER_ID);
        p.setId(10L);
        return p;
    }

    private Comment comment(long id, long postId, long authorId, Long parentId) {
        Comment c = new Comment(postId, authorId, "原始内容", parentId);
        c.setId(id);
        c.setStatus("PUBLISHED");
        c.setCreatedAt(LocalDateTime.now());
        return c;
    }

    @Nested
    @DisplayName("createComment")
    class CreateComment {

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenUserMissing() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(null);
            assertThrows(ResourceNotFoundException.class,
                    () -> commentService.createComment(USERNAME, 10L, new CreateCommentRequest("hi")));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when post not found")
        void shouldThrowWhenPostMissing() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            when(postRepository.findById(404L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> commentService.createComment(USERNAME, 404L, new CreateCommentRequest("hi")));
        }

        @Test
        @DisplayName("should sanitize XSS in content")
        void shouldSanitizeXss() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            when(postRepository.findById(10L)).thenReturn(Optional.of(samplePost()));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

            commentService.createComment(USERNAME, 10L,
                    new CreateCommentRequest("<script>alert(1)</script>"));

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).save(captor.capture());
            assertFalse(captor.getValue().getContent().contains("<script>"));
        }

        @Test
        @DisplayName("should notify post author when commenter is not the author")
        void shouldNotifyWhenNotAuthor() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Post post = new Post("标题", "正文", 999L); // different author
            post.setId(10L);
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(1L);
                return c;
            });
            when(userServiceClient.getUserById(999L))
                    .thenReturn(new UserInfoDTO(999L, "bob", "USER", null));

            commentService.createComment(USERNAME, 10L, new CreateCommentRequest("hi"));

            verify(notificationServiceClient).createNotification(any(CreateNotificationRequest.class));
        }

        @Test
        @DisplayName("should NOT notify post author when commenter is the author (self-comment)")
        void shouldNotNotifyWhenSelfComment() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            // author == user.getId()
            when(postRepository.findById(10L)).thenReturn(Optional.of(samplePost()));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(1L);
                return c;
            });

            commentService.createComment(USERNAME, 10L, new CreateCommentRequest("hi"));

            verify(notificationServiceClient, never()).createNotification(any());
        }

        @Test
        @DisplayName("should not throw if notification delivery fails (caught)")
        void shouldNotThrowOnNotificationFailure() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Post post = new Post("标题", "正文", 999L);
            post.setId(10L);
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(1L);
                return c;
            });
            when(userServiceClient.getUserById(999L))
                    .thenReturn(new UserInfoDTO(999L, "bob", "USER", null));
            doThrow(new RuntimeException("notification service down"))
                    .when(notificationServiceClient).createNotification(any());

            // Should NOT propagate
            assertDoesNotThrow(() ->
                    commentService.createComment(USERNAME, 10L, new CreateCommentRequest("hi")));
        }
    }

    @Nested
    @DisplayName("replyToComment")
    class ReplyToComment {

        @Test
        @DisplayName("should throw ResourceNotFoundException when parent comment not found")
        void shouldThrowWhenParentMissing() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            when(commentRepository.findById(404L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> commentService.replyToComment(USERNAME, 404L, new CreateCommentRequest("reply")));
        }

        @Test
        @DisplayName("should set parentId on the reply and sanitize XSS")
        void shouldSetParentIdAndSanitize() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Comment parent = comment(1L, 10L, 999L, null);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(parent));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(2L);
                return c;
            });

            commentService.replyToComment(USERNAME, 1L,
                    new CreateCommentRequest("<b>reply</b>"));

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).save(captor.capture());
            Comment saved = captor.getValue();
            assertEquals(1L, saved.getParentId());
            // <b> becomes &lt;b&gt;
            assertFalse(saved.getContent().contains("<b>"));
        }

        @Test
        @DisplayName("should notify the parent comment author when replier is not the author")
        void shouldNotifyParentAuthor() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Comment parent = comment(1L, 10L, 999L, null);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(parent));
            when(postRepository.findById(10L)).thenReturn(Optional.of(samplePost()));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(2L);
                return c;
            });

            commentService.replyToComment(USERNAME, 1L, new CreateCommentRequest("reply"));

            verify(notificationServiceClient).createNotification(any(CreateNotificationRequest.class));
        }

        @Test
        @DisplayName("should NOT notify when replying to own comment")
        void shouldNotNotifyWhenReplyingToSelf() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Comment parent = comment(1L, 10L, USER_ID, null); // same author
            when(commentRepository.findById(1L)).thenReturn(Optional.of(parent));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(2L);
                return c;
            });

            commentService.replyToComment(USERNAME, 1L, new CreateCommentRequest("reply"));

            verify(notificationServiceClient, never()).createNotification(any());
        }
    }

    @Nested
    @DisplayName("updateComment (ownership)")
    class UpdateComment {

        @Test
        @DisplayName("should throw UnauthorizedException when not the author")
        void shouldThrowWhenNotOwner() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Comment c = comment(1L, 10L, 999L, null);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(c));

            assertThrows(UnauthorizedException.class,
                    () -> commentService.updateComment(USERNAME, 1L, "new"));
        }

        @Test
        @DisplayName("should sanitize XSS on update when owner")
        void shouldSanitizeXssOnUpdate() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Comment c = comment(1L, 10L, USER_ID, null);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

            commentService.updateComment(USERNAME, 1L, "<img src=x onerror=alert(1)>");

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).save(captor.capture());
            assertFalse(captor.getValue().getContent().contains("<img"));
        }

        @Test
        @DisplayName("should update content when owner")
        void shouldUpdateWhenOwner() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Comment c = comment(1L, 10L, USER_ID, null);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

            CommentResponse resp = commentService.updateComment(USERNAME, 1L, "new content");
            assertEquals("new content", resp.getContent());
        }
    }

    @Nested
    @DisplayName("deleteComment (soft delete + cascade)")
    class DeleteComment {

        @Test
        @DisplayName("should throw UnauthorizedException when not the author")
        void shouldThrowWhenNotOwner() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Comment c = comment(1L, 10L, 999L, null);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
            assertThrows(UnauthorizedException.class,
                    () -> commentService.deleteComment(USERNAME, 1L));
        }

        @Test
        @DisplayName("should soft-delete (status=DELETED) and cascade to replies")
        void shouldSoftDeleteAndCascade() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Comment c = comment(1L, 10L, USER_ID, null);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
            Comment reply1 = comment(2L, 10L, 999L, 1L);
            Comment reply2 = comment(3L, 10L, 888L, 1L);
            when(commentRepository.findByParentIdOrderByCreatedAtAsc(1L))
                    .thenReturn(List.of(reply1, reply2));

            commentService.deleteComment(USERNAME, 1L);

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).save(captor.capture());
            assertEquals("DELETED", captor.getValue().getStatus());

            ArgumentCaptor<List<Comment>> listCaptor = ArgumentCaptor.forClass(List.class);
            verify(commentRepository).saveAll(listCaptor.capture());
            listCaptor.getValue().forEach(r -> assertEquals("DELETED", r.getStatus()));
        }

        @Test
        @DisplayName("should work when comment has no replies")
        void shouldDeleteWithoutReplies() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Comment c = comment(1L, 10L, USER_ID, null);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
            when(commentRepository.findByParentIdOrderByCreatedAtAsc(1L))
                    .thenReturn(Collections.emptyList());

            commentService.deleteComment(USERNAME, 1L);

            verify(commentRepository).save(any(Comment.class));
            verify(commentRepository).saveAll(Collections.emptyList());
        }
    }

    @Nested
    @DisplayName("getCommentsByPostId (tree building)")
    class GetCommentsByPostId {

        @Test
        @DisplayName("should throw ResourceNotFoundException when post not found")
        void shouldThrowWhenPostMissing() {
            when(postRepository.findById(404L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> commentService.getCommentsByPostId(404L));
        }

        @Test
        @DisplayName("should build tree with replies nested under root comments")
        void shouldBuildTree() {
            Post post = samplePost();
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            Comment root = comment(1L, 10L, USER_ID, null);
            Comment reply = comment(2L, 10L, 999L, 1L);
            when(commentRepository.findByPostIdAndStatusOrderByCreatedAtAsc(10L, "PUBLISHED"))
                    .thenReturn(List.of(root, reply));
            when(userServiceClient.getUsersByIds(any()))
                    .thenReturn(List.of(
                            new UserInfoDTO(USER_ID, USERNAME, "USER", "https://cdn/alice.png"),
                            new UserInfoDTO(999L, "bob", "USER", null)));

            List<CommentResponse> result = commentService.getCommentsByPostId(10L);

            assertEquals(1, result.size()); // only one root
            CommentResponse rootResp = result.get(0);
            assertEquals(1L, rootResp.getId());
            assertEquals(1, rootResp.getReplies().size());
            assertEquals(2L, rootResp.getReplies().get(0).getId());
        }

        @Test
        @DisplayName("should return empty list when no comments")
        void shouldReturnEmptyWhenNoComments() {
            Post post = samplePost();
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(commentRepository.findByPostIdAndStatusOrderByCreatedAtAsc(10L, "PUBLISHED"))
                    .thenReturn(Collections.emptyList());

            List<CommentResponse> result = commentService.getCommentsByPostId(10L);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getMyComments")
    class GetMyComments {

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowWhenUserMissing() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(null);
            assertThrows(ResourceNotFoundException.class,
                    () -> commentService.getMyComments(USERNAME));
        }

        @Test
        @DisplayName("should return empty list when user has no comments")
        void shouldReturnEmptyWhenNoComments() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            when(commentRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(USER_ID, "PUBLISHED"))
                    .thenReturn(Collections.emptyList());
            assertTrue(commentService.getMyComments(USERNAME).isEmpty());
        }

        @Test
        @DisplayName("should join with post title and return UserCommentResponse list")
        void shouldJoinWithPostTitle() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Comment c1 = comment(1L, 10L, USER_ID, null);
            Comment c2 = comment(2L, 20L, USER_ID, 5L);
            when(commentRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(USER_ID, "PUBLISHED"))
                    .thenReturn(List.of(c1, c2));

            Post p1 = samplePost();
            p1.setId(10L);
            p1.setTitle("First post");
            Post p2 = samplePost();
            p2.setId(20L);
            p2.setTitle("Second post");
            when(postRepository.findAllById(any())).thenReturn(List.of(p1, p2));

            List<UserCommentResponse> result = commentService.getMyComments(USERNAME);
            assertEquals(2, result.size());
            // Should map post title correctly
            assertTrue(result.stream().anyMatch(r -> "First post".equals(r.getPostTitle())));
            assertTrue(result.stream().anyMatch(r -> "Second post".equals(r.getPostTitle())));
        }

        @Test
        @DisplayName("should use '未知帖子' when post is missing from lookup")
        void shouldFallbackWhenPostMissing() {
            when(userServiceClient.getUserByUsername(USERNAME)).thenReturn(user());
            Comment c1 = comment(1L, 10L, USER_ID, null);
            when(commentRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(USER_ID, "PUBLISHED"))
                    .thenReturn(List.of(c1));
            when(postRepository.findAllById(any())).thenReturn(Collections.emptyList());

            List<UserCommentResponse> result = commentService.getMyComments(USERNAME);
            assertEquals(1, result.size());
            assertEquals("未知帖子", result.get(0).getPostTitle());
        }
    }
}
