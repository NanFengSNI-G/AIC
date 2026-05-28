package com.project.demo.service;

import java.util.List;
import java.util.Map;

public interface RedisLikeService {

    // ===== 帖子点赞 =====

    void likePost(Long postId, Long userId);

    void unlikePost(Long postId, Long userId);

    boolean hasLikedPost(Long postId, Long userId);

    /** 批量查询当前用户是否点赞了指定帖子 */
    Map<Long, Boolean> hasLikedPostsBatch(List<Long> postIds, Long userId);

    // ===== 评论点赞 =====

    void likeComment(Long commentId, Long userId);

    void unlikeComment(Long commentId, Long userId);

    boolean hasLikedComment(Long commentId, Long userId);

    /** 批量查询当前用户是否点赞了指定评论 */
    Map<Long, Boolean> hasLikedCommentsBatch(List<Long> commentIds, Long userId);

    // ===== 浏览数 =====

    /** 增加帖子浏览数（Redis累计） */
    void incrementViewCount(Long postId);

    /** 获取并清除Redis中的浏览数增量 */
    Map<Long, Long> drainViewCounts();
}