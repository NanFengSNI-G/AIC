package com.project.demo.service;

import com.project.demo.dto.*;

import java.util.List;

public interface ForumService {

    // ===== 板块 =====
    List<SectionResponse> getSectionList();

    // ===== 帖子 =====
    PageResponse<PostResponse> getPostList(Long userId, Long sectionId, String sortBy, int page, int size);

    PostResponse getPostById(Long postId, Long userId);

    PostResponse createPost(Long userId, CreatePostRequest request);

    PostResponse updatePost(Long userId, Long postId, UpdatePostRequest request);

    void deletePost(Long userId, Long postId);

    // ===== 点赞 =====
    boolean toggleLike(Long userId, Long postId);

    // ===== 收藏 =====
    boolean toggleFavorite(Long userId, Long postId);

    PageResponse<PostResponse> getUserFavorites(Long userId, int page, int size);

    PageResponse<PostResponse> getUserPosts(Long userId, int page, int size);

    // ===== 搜索 =====
    PageResponse<PostResponse> searchPosts(Long userId, String keyword, Long sectionId, String sortBy, int page, int size);

    // ===== 评论 =====
    PageResponse<CommentResponse> getComments(Long userId, Long postId, int page, int size);

    CommentResponse createComment(Long userId, Long postId, CreateCommentRequest request);

    void deleteComment(Long userId, Long commentId);

    boolean toggleCommentLike(Long userId, Long commentId);
}
