package com.project.demo.controller;

import com.project.demo.dto.*;
import com.project.demo.entity.UserDetailsImpl;
import com.project.demo.service.ForumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;

    // ===== 板块 =====

    // 获取板块列表 GET /api/forum/sections
    @GetMapping("/sections")
    public ApiResponse<List<SectionResponse>> getSections() {
        return ApiResponse.success(forumService.getSectionList());
    }

    // ===== 帖子 =====

    // 获取帖子列表 GET /api/forum/posts?sectionId=1&sortBy=latest&page=1&size=20
    @GetMapping("/posts")
    public ApiResponse<PageResponse<PostResponse>> getPosts(
            @RequestParam(required = false) Long sectionId,
            @RequestParam(defaultValue = "latest") String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUserIdOrNull();
        return ApiResponse.success(forumService.getPostList(userId, sectionId, sortBy, page, size));
    }

    // 搜索帖子 GET /api/forum/search?keyword=xxx&sectionId=1&sortBy=relevance&page=1&size=20
    @GetMapping("/search")
    public ApiResponse<PageResponse<PostResponse>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(defaultValue = "relevance") String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUserIdOrNull();
        return ApiResponse.success(forumService.searchPosts(userId, keyword, sectionId, sortBy, page, size));
    }

    // 获取帖子详情 GET /api/forum/post/{id}
    @GetMapping("/post/{id}")
    public ApiResponse<PostResponse> getPost(@PathVariable Long id) {
        Long userId = getCurrentUserIdOrNull();
        return ApiResponse.success(forumService.getPostById(id, userId));
    }

    // 发布帖子 POST /api/forum/post
    @PostMapping("/post")
    public ApiResponse<PostResponse> createPost(@Valid @RequestBody CreatePostRequest request) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(forumService.createPost(userId, request));
    }

    // 编辑帖子 PUT /api/forum/post/{id}
    @PutMapping("/post/{id}")
    public ApiResponse<PostResponse> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(forumService.updatePost(userId, id, request));
    }

    // 删除帖子 DELETE /api/forum/post/{id}
    @DeleteMapping("/post/{id}")
    public ApiResponse<Void> deletePost(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        forumService.deletePost(userId, id);
        return ApiResponse.success(null);
    }

    // 点赞帖子 POST /api/forum/post/{id}/like
    @PostMapping("/post/{id}/like")
    public ApiResponse<Map<String, Boolean>> toggleLike(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        boolean liked = forumService.toggleLike(userId, id);
        return ApiResponse.success(Map.of("liked", liked));
    }

    // 收藏帖子 POST /api/forum/post/{id}/favorite
    @PostMapping("/post/{id}/favorite")
    public ApiResponse<Map<String, Boolean>> toggleFavorite(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        boolean favorited = forumService.toggleFavorite(userId, id);
        return ApiResponse.success(Map.of("favorited", favorited));
    }

    // 获取用户收藏列表 GET /api/forum/user/favorites?page=1&size=20
    @GetMapping("/user/favorites")
    public ApiResponse<PageResponse<PostResponse>> getUserFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(forumService.getUserFavorites(userId, page, size));
    }

    // 获取用户帖子列表 GET /api/forum/user/posts?userId=1&page=1&size=20
    @GetMapping("/user/posts")
    public ApiResponse<PageResponse<PostResponse>> getUserPosts(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(forumService.getUserPosts(userId, page, size));
    }

    // ===== 评论 =====

    // 获取评论列表 GET /api/forum/post/{id}/comments?page=1&size=20
    @GetMapping("/post/{id}/comments")
    public ApiResponse<PageResponse<CommentResponse>> getComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUserIdOrNull();
        return ApiResponse.success(forumService.getComments(userId, id, page, size));
    }

    // 发表评论 POST /api/forum/post/{id}/comment
    @PostMapping("/post/{id}/comment")
    public ApiResponse<CommentResponse> createComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(forumService.createComment(userId, id, request));
    }

    // 删除评论 DELETE /api/forum/comment/{id}
    @DeleteMapping("/comment/{id}")
    public ApiResponse<Void> deleteComment(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        forumService.deleteComment(userId, id);
        return ApiResponse.success(null);
    }

    // 点赞评论 POST /api/forum/comment/{id}/like
    @PostMapping("/comment/{id}/like")
    public ApiResponse<Map<String, Boolean>> toggleCommentLike(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        boolean liked = forumService.toggleCommentLike(userId, id);
        return ApiResponse.success(Map.of("liked", liked));
    }
    // ===== 工具方法 =====

    private Long getCurrentUserId() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return userDetails.getUserId();
    }

    private Long getCurrentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() ||
                "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        return userDetails.getUserId();
    }
}
