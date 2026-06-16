package com.cc91.forumservice.fuzz;

import com.cc91.forumservice.client.UserServiceClient;
import com.cc91.forumservice.dto.PostResponse;
import com.cc91.forumservice.entity.Post;
import com.cc91.forumservice.repository.BookmarkRepository;
import com.cc91.forumservice.repository.CategoryRepository;
import com.cc91.forumservice.repository.CommentRepository;
import com.cc91.forumservice.repository.PostLikeRepository;
import com.cc91.forumservice.repository.PostRepository;
import com.cc91.forumservice.service.PostService;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Property-based fuzz tests for {@link PostService#searchPosts}.
 *
 * Robustness invariant: for any keyword string (including SQL-meta characters that
 * would break naive concatenation), searchPosts must return a Page rather than throw
 * SQLException / any other RuntimeException. The repository is mocked so JPA
 * parameterization is exercised via Mockito matchers (anyString()), proving the
 * service never assembles raw SQL.
 *
 * No Spring context — pure jqwik + Mockito, mirroring PostServiceTest's setup.
 * Note: jqwik @Property methods do not invoke JUnit's @BeforeEach, so we build
 * the mocks + service inside each property method body (via newSearchService()).
 */
class PostServiceSearchFuzzTest {

    /**
     * Build a fresh PostService with all dependencies mocked. Repository is stubbed
     * to return an empty Page for any (status, keyword) pair — searchPosts must
     * complete normally regardless of the keyword contents.
     */
    private static PostService newSearchService() {
        PostRepository postRepository = mock(PostRepository.class);
        Page<Post> emptyPage = new PageImpl<>(Collections.emptyList());
        when(postRepository.findByStatusAndTitleContainingOrStatusAndContentContaining(
                anyString(), anyString(), anyString(), anyString(), any(Pageable.class)))
                .thenReturn(emptyPage);

        return new PostService(
                postRepository,
                mock(PostLikeRepository.class),
                mock(BookmarkRepository.class),
                mock(UserServiceClient.class),
                mock(CategoryRepository.class),
                mock(CommentRepository.class));
    }

    /** Variant that returns the underlying postRepository mock for verify assertions. */
    private static SearchServiceWithRepo newSearchServiceAndRepo() {
        PostRepository postRepository = mock(PostRepository.class);
        Page<Post> emptyPage = new PageImpl<>(Collections.emptyList());
        when(postRepository.findByStatusAndTitleContainingOrStatusAndContentContaining(
                anyString(), anyString(), anyString(), anyString(), any(Pageable.class)))
                .thenReturn(emptyPage);

        PostService service = new PostService(
                postRepository,
                mock(PostLikeRepository.class),
                mock(BookmarkRepository.class),
                mock(UserServiceClient.class),
                mock(CategoryRepository.class),
                mock(CommentRepository.class));
        return new SearchServiceWithRepo(service, postRepository);
    }

    private record SearchServiceWithRepo(PostService service, PostRepository repo) {}

    @Provide
    Arbitrary<String> keywords() {
        Arbitrary<String> sqlPayloads = Arbitraries.of(
                "'--", ";--", "%", "_", ";", "\"", "\\", "' OR 1=1 --",
                "'; DROP TABLE posts; --", "%_%", "_'", "\" OR \"=\"", " ",
                "UNION SELECT", "/*", "*/", "@@version", "xp_cmdshell",
                "%' OR '1'='1", "\n", "\t", "  ", "🔑", "<script>alert(1)</script>");
        Arbitrary<String> random = Arbitraries.strings()
                .ofMinLength(1).ofMaxLength(200)
                .withChars("'\";\\%<>& \n\r\t()");
        return Arbitraries.oneOf(sqlPayloads, sqlPayloads, random);
    }

    @Provide
    Arbitrary<String> blankKeywords() {
        return Arbitraries.of("", "   ", "\t", "\n", "  \t  ");
    }

    /**
     * 300 tries: any keyword (incl. SQL meta-chars) must produce a non-null Page
     * without throwing. JPA parameterized queries neutralize the payload.
     */
    @Property(tries = 300)
    void searchNeverThrowsOnAnyKeyword(@ForAll("keywords") String keyword) {
        PostService service = newSearchService();
        Page<PostResponse> result = assertDoesNotThrow(
                () -> service.searchPosts(keyword, 0, 10));
        assertNotNull(result);
    }

    /**
     * Blank/whitespace-only keyword is a special path — service returns Page.empty()
     * without ever calling the repository.
     */
    @Property(tries = 50)
    void searchOnBlankKeywordReturnsEmptyPage(@ForAll("blankKeywords") String keyword) {
        SearchServiceWithRepo pair = newSearchServiceAndRepo();
        Page<PostResponse> result = assertDoesNotThrow(
                () -> pair.service().searchPosts(keyword, 0, 10));
        assertNotNull(result);
        verifyNoInteractions(pair.repo());
    }
}
