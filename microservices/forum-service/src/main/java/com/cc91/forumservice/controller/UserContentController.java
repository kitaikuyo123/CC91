package com.cc91.forumservice.controller;

import com.cc91.forumservice.dto.PostResponse;
import com.cc91.forumservice.dto.UserCommentResponse;
import com.cc91.forumservice.exception.UnauthorizedException;
import com.cc91.forumservice.service.CommentService;
import com.cc91.forumservice.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户内容控制器
 * 处理当前用户的帖子、草稿、评论、收藏等请求
 */
@RestController
@RequestMapping("/api/users")
public class UserContentController {

    private final PostService postService;
    private final CommentService commentService;

    public UserContentController(PostService postService, CommentService commentService) {
        this.postService = postService;
        this.commentService = commentService;
    }

    @GetMapping("/me/posts")
    public ResponseEntity<List<PostResponse>> getMyPosts() {
        String username = getCurrentUsername();
        List<PostResponse> posts = postService.getMyPosts(username);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/me/drafts")
    public ResponseEntity<List<PostResponse>> getMyDrafts() {
        String username = getCurrentUsername();
        List<PostResponse> drafts = postService.getMyDrafts(username);
        return ResponseEntity.ok(drafts);
    }

    @GetMapping("/me/comments")
    public ResponseEntity<List<UserCommentResponse>> getMyComments() {
        String username = getCurrentUsername();
        List<UserCommentResponse> comments = commentService.getMyComments(username);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/me/bookmarks")
    public ResponseEntity<List<PostResponse>> getMyBookmarks() {
        String username = getCurrentUsername();
        List<PostResponse> bookmarks = postService.getMyBookmarks(username);
        return ResponseEntity.ok(bookmarks);
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        throw new UnauthorizedException("用户未登录");
    }
}
