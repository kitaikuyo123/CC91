package com.cc91.service;

import com.cc91.dto.CreatePostRequest;
import com.cc91.dto.PostResponse;
import com.cc91.dto.UpdatePostRequest;
import com.cc91.entity.Category;
import com.cc91.entity.Post;
import com.cc91.entity.User;
import com.cc91.exception.ResourceNotFoundException;
import com.cc91.exception.UnauthorizedException;
import com.cc91.repository.PostRepository;
import com.cc91.repository.UserRepository;
import com.cc91.repository.CategoryRepository;
import com.cc91.repository.CommentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 帖子服务
 */
@Service
public class PostService {

    private static final Logger logger = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository, CategoryRepository categoryRepository, CommentRepository commentRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.commentRepository = commentRepository;
    }

    /**
     * 创建帖子
     */
    @Transactional
    public PostResponse createPost(String username, CreatePostRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        Post post = new Post(request.getTitle(), request.getContent(), user.getId());
        post.setCategoryId(request.getCategoryId());
        if (request.getStatus() != null) {
            post.setStatus(request.getStatus());
        }
        post = postRepository.save(post);

        logger.info("帖子创建成功: id={}, author={}", post.getId(), username);

        return toPostResponse(post, user.getUsername());
    }

    /**
     * 获取帖子详情（增加浏览次数）
     */
    @Transactional
    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));

        // 增加浏览次数
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);

        User author = userRepository.findById(post.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("作者不存在"));

        return toPostResponse(post, author.getUsername());
    }

    /**
     * 根据版块ID分页查询帖子
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getPostsByCategory(Long categoryId, int page, int size) {
        // 验证版块是否存在
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
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));

        // 验证作者权限
        if (!post.getAuthorId().equals(user.getId())) {
            throw new UnauthorizedException("无权限编辑此帖子");
        }

        // 更新字段
        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }
        if (request.getCategoryId() != null) {
            // 验证版块是否存在
            if (!categoryRepository.existsById(request.getCategoryId())) {
                throw new ResourceNotFoundException("版块不存在");
            }
            post.setCategoryId(request.getCategoryId());
        }

        post = postRepository.save(post);

        logger.info("帖子更新成功: id={}, author={}", postId, username);

        return toPostResponse(post, user.getUsername());
    }

    /**
     * 删除帖子
     */
    @Transactional
    public void deletePost(String username, Long postId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));

        // 验证作者权限
        if (!post.getAuthorId().equals(user.getId())) {
            throw new UnauthorizedException("无权限删除此帖子");
        }

        postRepository.delete(post);

        logger.info("帖子删除成功: id={}, author={}", postId, username);
    }

    /**
     * 分页查询帖子列表
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getPostList(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> posts;
        if (status == null || status.trim().isEmpty()) {
            posts = postRepository.findAll(pageable);
        } else {
            posts = postRepository.findByStatus(status, pageable);
        }

        return toPostResponsePage(posts);
    }

    /**
     * 搜索帖子（按标题或内容）
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> searchPosts(String keyword, int page, int size) {
        // 空关键词返回空结果
        if (keyword == null || keyword.trim().isEmpty()) {
            return Page.empty();
        }

        String trimmedKeyword = keyword.trim();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 搜索标题或内容包含关键词的已发布帖子
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

        // 批量收集所有需要的 id
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

        // 批量查询用户
        Map<Long, User> userMap = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 批量查询分类
        Map<Long, Category> categoryMap = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : categoryRepository.findAllById(categoryIds).stream()
                        .collect(Collectors.toMap(Category::getId, c -> c));

        // 批量查询评论数
        Map<Long, Long> commentCountMap = new HashMap<>();
        for (Long postId : postIds) {
            commentCountMap.put(postId, commentRepository.countByPostIdAndStatus(postId, "PUBLISHED"));
        }

        return posts.map(post -> {
            User author = userMap.get(post.getAuthorId());
            String authorUsername = author != null ? author.getUsername() : "未知用户";

            String categoryName = null;
            if (post.getCategoryId() != null) {
                Category category = categoryMap.get(post.getCategoryId());
                categoryName = category != null ? category.getName() : null;
            }

            long commentCount = commentCountMap.getOrDefault(post.getId(), 0L);

            return new PostResponse(
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
        });
    }

    /**
     * 转换单个 Post 为 PostResponse（用于详情等单条查询场景）
     */
    private PostResponse toPostResponse(Post post, String authorUsername) {
        String categoryName = null;
        if (post.getCategoryId() != null) {
            Category category = categoryRepository.findById(post.getCategoryId()).orElse(null);
            categoryName = category != null ? category.getName() : null;
        }

        long commentCount = commentRepository.countByPostIdAndStatus(post.getId(), "PUBLISHED");

        return new PostResponse(
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
    }
}
