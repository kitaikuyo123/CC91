package com.cc91.forumservice.service;

import com.cc91.forumservice.client.UserInfoDTO;
import com.cc91.forumservice.client.UserServiceClient;
import com.cc91.forumservice.dto.CreatePostRequest;
import com.cc91.forumservice.dto.PostResponse;
import com.cc91.forumservice.dto.UpdatePostRequest;
import com.cc91.forumservice.entity.Bookmark;
import com.cc91.forumservice.entity.Category;
import com.cc91.forumservice.entity.Post;
import com.cc91.forumservice.entity.PostLike;
import com.cc91.forumservice.exception.ResourceNotFoundException;
import com.cc91.forumservice.util.HtmlSanitizer;
import com.cc91.forumservice.exception.UnauthorizedException;
import com.cc91.forumservice.repository.PostRepository;
import com.cc91.forumservice.repository.PostLikeRepository;
import com.cc91.forumservice.repository.BookmarkRepository;
import com.cc91.forumservice.repository.CategoryRepository;
import com.cc91.forumservice.repository.CommentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 帖子服务
 */
@Service
public class PostService {

    private static final Logger logger = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final BookmarkRepository bookmarkRepository;
    private final UserServiceClient userServiceClient;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;

    public PostService(PostRepository postRepository,
                       PostLikeRepository postLikeRepository,
                       BookmarkRepository bookmarkRepository,
                       UserServiceClient userServiceClient,
                       CategoryRepository categoryRepository,
                       CommentRepository commentRepository) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.userServiceClient = userServiceClient;
        this.categoryRepository = categoryRepository;
        this.commentRepository = commentRepository;
    }

    /**
     * 创建帖子
     */
    @Transactional
    public PostResponse createPost(String username, CreatePostRequest request) {
        UserInfoDTO user = userServiceClient.getUserByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new ResourceNotFoundException("版块不存在");
        }

        Post post = new Post(
                HtmlSanitizer.sanitizeContent(request.getTitle()),
                HtmlSanitizer.sanitizeContent(request.getContent()),
                user.getId()
        );
        post.setCategoryId(request.getCategoryId());
        if (request.getStatus() != null) {
            post.setStatus(request.getStatus());
        }
        post = postRepository.save(post);

        logger.info("帖子创建成功: id={}, author={}", post.getId(), username);

        return toPostResponse(post, user.getUsername(), user.getAvatarUrl());
    }

    /**
     * 获取帖子详情（可选增加浏览次数）
     */
    @Transactional
    public PostResponse getPostById(Long id, boolean increaseView) {
        if (increaseView) {
            postRepository.incrementViewCount(id);
        }

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));

        UserInfoDTO author = userServiceClient.getUserById(post.getAuthorId());
        if (author == null) {
            throw new ResourceNotFoundException("作者不存在");
        }

        return toPostResponse(post, author.getUsername(), author.getAvatarUrl());
    }

    /**
     * 根据版块ID分页查询帖子
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getPostsByCategory(Long categoryId, int page, int size) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("版块不存在");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> posts = postRepository.findByCategoryIdAndStatus(categoryId, "PUBLISHED", pageable);

        return toPostResponsePage(posts);
    }

    /**
     * 更新帖子
     */
    @Transactional
    public PostResponse updatePost(String username, Long postId, UpdatePostRequest request) {
        UserInfoDTO user = userServiceClient.getUserByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));

        if (!post.getAuthorId().equals(user.getId())) {
            throw new UnauthorizedException("无权限编辑此帖子");
        }

        if (request.getTitle() != null) {
            post.setTitle(HtmlSanitizer.sanitizeContent(request.getTitle()));
        }
        if (request.getContent() != null) {
            post.setContent(HtmlSanitizer.sanitizeContent(request.getContent()));
        }
        if (request.getCategoryId() != null) {
            if (!categoryRepository.existsById(request.getCategoryId())) {
                throw new ResourceNotFoundException("版块不存在");
            }
            post.setCategoryId(request.getCategoryId());
        }
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            post.setStatus(request.getStatus().trim());
        }

        post = postRepository.save(post);

        logger.info("帖子更新成功: id={}, author={}", postId, username);

        return toPostResponse(post, user.getUsername(), user.getAvatarUrl());
    }

    /**
     * 删除帖子
     */
    @Transactional
    public void deletePost(String username, Long postId) {
        UserInfoDTO user = userServiceClient.getUserByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));

        if (!post.getAuthorId().equals(user.getId())) {
            throw new UnauthorizedException("无权限删除此帖子");
        }

        postRepository.delete(post);

        logger.info("帖子删除成功: id={}, author={}", postId, username);
    }

    /**
     * 切换点赞状态（点赞/取消点赞）
     */
    @Transactional
    public Map<String, Object> toggleLike(String username, Long postId) {
        UserInfoDTO user = userServiceClient.getUserByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));

        Optional<PostLike> existingLike = postLikeRepository.findByUserIdAndPostId(user.getId(), postId);
        boolean isLiked;

        if (existingLike.isPresent()) {
            postLikeRepository.deleteByUserIdAndPostId(user.getId(), postId);
            isLiked = false;
            logger.info("取消点赞: userId={}, postId={}", user.getId(), postId);
        } else {
            postLikeRepository.save(new PostLike(user.getId(), postId));
            isLiked = true;
            logger.info("点赞: userId={}, postId={}", user.getId(), postId);
        }

        long likeCount = postLikeRepository.countByPostId(postId);
        post.setLikeCount((int) likeCount);
        postRepository.save(post);

        Map<String, Object> result = new HashMap<>();
        result.put("isLiked", isLiked);
        result.put("likeCount", likeCount);
        return result;
    }

    /**
     * 切换收藏状态（收藏/取消收藏）
     */
    @Transactional
    public Map<String, Object> toggleBookmark(String username, Long postId) {
        UserInfoDTO user = userServiceClient.getUserByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));

        Optional<Bookmark> existingBookmark = bookmarkRepository.findByUserIdAndPostId(user.getId(), postId);
        boolean isBookmarked;

        if (existingBookmark.isPresent()) {
            bookmarkRepository.deleteByUserIdAndPostId(user.getId(), postId);
            isBookmarked = false;
            logger.info("取消收藏: userId={}, postId={}", user.getId(), postId);
        } else {
            bookmarkRepository.save(new Bookmark(user.getId(), postId));
            isBookmarked = true;
            logger.info("收藏: userId={}, postId={}", user.getId(), postId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("isBookmarked", isBookmarked);
        return result;
    }

    /**
     * 获取当前用户收藏的帖子列表
     */
    @Transactional(readOnly = true)
    public List<PostResponse> getMyBookmarks(String username) {
        UserInfoDTO user = userServiceClient.getUserByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        List<Bookmark> bookmarks = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (bookmarks.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> postIds = bookmarks.stream()
                .map(Bookmark::getPostId)
                .collect(Collectors.toList());

        List<Post> posts = postRepository.findAllById(postIds);

        Map<Long, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        List<Post> orderedPosts = postIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return toPostResponseList(orderedPosts);
    }

    /**
     * 分页查询帖子列表（支持排序）
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getPostList(int page, int size, String status, String sort) {
        Sort sorting;
        if ("comments".equalsIgnoreCase(sort)) {
            sorting = Sort.by(Sort.Direction.DESC, "commentCount");
        } else if ("hot".equalsIgnoreCase(sort)) {
            sorting = Sort.by(Sort.Direction.DESC, "viewCount");
        } else {
            sorting = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<Post> posts;
        if (status == null || status.trim().isEmpty()) {
            posts = postRepository.findAll(pageable);
        } else {
            posts = postRepository.findByStatus(status, pageable);
        }

        return toPostResponsePage(posts);
    }

    /**
     * 分页查询帖子列表（无排序参数，默认按时间倒序）
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getPostList(int page, int size, String status) {
        return getPostList(page, size, status, "latest");
    }

    /**
     * 获取当前用户已发布的帖子列表（用于 Dashboard "我的帖子"）
     */
    @Transactional(readOnly = true)
    public List<PostResponse> getMyPosts(String username) {
        UserInfoDTO user = userServiceClient.getUserByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        List<Post> posts = postRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(user.getId(), "PUBLISHED");
        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        return toPostResponseList(posts);
    }

    /**
     * 获取当前用户的草稿列表
     */
    @Transactional(readOnly = true)
    public List<PostResponse> getMyDrafts(String username) {
        UserInfoDTO user = userServiceClient.getUserByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        List<Post> posts = postRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(user.getId(), "DRAFT");
        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        return toPostResponseList(posts);
    }

    /**
     * 搜索帖子（按标题或内容）
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> searchPosts(String keyword, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Page.empty();
        }

        String trimmedKeyword = keyword.trim();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Post> posts = postRepository.findByStatusAndTitleContainingOrStatusAndContentContaining(
                "PUBLISHED", trimmedKeyword,
                "PUBLISHED", trimmedKeyword,
                pageable
        );

        return toPostResponsePage(posts);
    }

    /**
     * 批量转换 Post Page 为 PostResponse Page（解决 N+1 查询问题）
     */
    private Page<PostResponse> toPostResponsePage(Page<Post> posts) {
        List<Post> postList = posts.getContent();

        Set<Long> authorIds = postList.stream()
                .map(Post::getAuthorId)
                .collect(Collectors.toSet());

        Set<Long> categoryIds = postList.stream()
                .map(Post::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> postIds = postList.stream()
                .map(Post::getId)
                .collect(Collectors.toSet());

        Map<Long, UserInfoDTO> userMap = batchResolveUsers(authorIds);

        Map<Long, Category> categoryMap = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : categoryRepository.findAllById(categoryIds).stream()
                        .collect(Collectors.toMap(Category::getId, c -> c));

        Map<Long, Long> commentCountMap = batchCountComments(postIds);

        return posts.map(post -> {
            UserInfoDTO author = userMap.get(post.getAuthorId());
            String authorUsername = author != null ? author.getUsername() : "未知用户";
            String avatarUrl = author != null ? author.getAvatarUrl() : null;

            String categoryName = null;
            if (post.getCategoryId() != null) {
                Category category = categoryMap.get(post.getCategoryId());
                categoryName = category != null ? category.getName() : null;
            }

            long commentCount = commentCountMap.getOrDefault(post.getId(), 0L);

            PostResponse response = new PostResponse(
                    post.getId(),
                    post.getTitle(),
                    post.getContent(),
                    post.getAuthorId(),
                    authorUsername,
                    post.getCategoryId(),
                    categoryName,
                    post.getCreatedAt(),
                    post.getUpdatedAt(),
                    post.getViewCount(),
                    post.getStatus(),
                    commentCount
            );
            response.setAuthorAvatarUrl(normalizeAvatarUrl(avatarUrl));
            response.setLikeCount(post.getLikeCount() != null ? post.getLikeCount().longValue() : 0L);
            response.setIsLikedByCurrentUser(false);
            response.setIsBookmarkedByCurrentUser(false);
            return response;
        });
    }

    /**
     * 批量转换 Post List 为 PostResponse List（解决 N+1 查询问题）
     */
    private List<PostResponse> toPostResponseList(List<Post> postList) {
        Set<Long> authorIds = postList.stream()
                .map(Post::getAuthorId)
                .collect(Collectors.toSet());

        Set<Long> categoryIds = postList.stream()
                .map(Post::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> postIds = postList.stream()
                .map(Post::getId)
                .collect(Collectors.toSet());

        Map<Long, UserInfoDTO> userMap = batchResolveUsers(authorIds);

        Map<Long, Category> categoryMap = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : categoryRepository.findAllById(categoryIds).stream()
                        .collect(Collectors.toMap(Category::getId, c -> c));

        Map<Long, Long> commentCountMap = batchCountComments(postIds);

        return postList.stream().map(post -> {
            UserInfoDTO author = userMap.get(post.getAuthorId());
            String authorUsername = author != null ? author.getUsername() : "未知用户";
            String avatarUrl = author != null ? author.getAvatarUrl() : null;

            String categoryName = null;
            if (post.getCategoryId() != null) {
                Category category = categoryMap.get(post.getCategoryId());
                categoryName = category != null ? category.getName() : null;
            }

            long commentCount = commentCountMap.getOrDefault(post.getId(), 0L);

            PostResponse response = new PostResponse(
                    post.getId(),
                    post.getTitle(),
                    post.getContent(),
                    post.getAuthorId(),
                    authorUsername,
                    post.getCategoryId(),
                    categoryName,
                    post.getCreatedAt(),
                    post.getUpdatedAt(),
                    post.getViewCount(),
                    post.getStatus(),
                    commentCount
            );
            response.setAuthorAvatarUrl(normalizeAvatarUrl(avatarUrl));
            response.setLikeCount(post.getLikeCount() != null ? post.getLikeCount().longValue() : 0L);
            response.setIsLikedByCurrentUser(false);
            response.setIsBookmarkedByCurrentUser(false);
            return response;
        }).collect(Collectors.toList());
    }

    /**
     * 转换单个 Post 为 PostResponse（用于详情等单条查询场景）
     */
    private PostResponse toPostResponse(Post post, String authorUsername, String avatarUrl) {
        String categoryName = null;
        if (post.getCategoryId() != null) {
            Category category = categoryRepository.findById(post.getCategoryId()).orElse(null);
            categoryName = category != null ? category.getName() : null;
        }

        long commentCount = commentRepository.countByPostIdAndStatus(post.getId(), "PUBLISHED");

        PostResponse response = new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthorId(),
                authorUsername,
                post.getCategoryId(),
                categoryName,
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getViewCount(),
                post.getStatus(),
                commentCount
        );

        response.setAuthorAvatarUrl(normalizeAvatarUrl(avatarUrl));
        response.setLikeCount(post.getLikeCount() != null ? post.getLikeCount().longValue() : 0L);
        response.setIsLikedByCurrentUser(false);
        response.setIsBookmarkedByCurrentUser(false);

        return response;
    }

    private static String normalizeAvatarUrl(String avatarUrl) {
        return avatarUrl != null && !avatarUrl.isEmpty() ? avatarUrl : null;
    }

    /**
     * Batch resolve user info via a single Feign call instead of N individual calls.
     */
    private Map<Long, UserInfoDTO> batchResolveUsers(Set<Long> authorIds) {
        if (authorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> idList = new ArrayList<>(authorIds);
        List<UserInfoDTO> users = userServiceClient.getUsersByIds(idList);
        return users.stream()
                .filter(u -> u.getId() != null)
                .collect(Collectors.toMap(UserInfoDTO::getId, u -> u));
    }

    /**
     * Batch count comments for multiple posts in a single query.
     */
    private Map<Long, Long> batchCountComments(Set<Long> postIds) {
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return commentRepository.countByPostIdInAndStatus(postIds, "PUBLISHED")
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
