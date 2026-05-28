package com.project.demo.service.impl;

import com.project.demo.service.RedisLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RedisLikeServiceImpl implements RedisLikeService {

    private static final String POST_LIKE_KEY_PREFIX = "forum:likes:post:";
    private static final String COMMENT_LIKE_KEY_PREFIX = "forum:likes:comment:";
    private static final String VIEW_COUNT_KEY = "forum:view_deltas";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ===== 帖子点赞 =====

    @Override
    public void likePost(Long postId, Long userId) {
        String key = POST_LIKE_KEY_PREFIX + postId;
        redisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
    }

    @Override
    public void unlikePost(Long postId, Long userId) {
        String key = POST_LIKE_KEY_PREFIX + postId;
        redisTemplate.opsForZSet().remove(key, userId.toString());
    }

    @Override
    public boolean hasLikedPost(Long postId, Long userId) {
        String key = POST_LIKE_KEY_PREFIX + postId;
        Double score = redisTemplate.opsForZSet().score(key, userId.toString());
        return score != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Long, Boolean> hasLikedPostsBatch(List<Long> postIds, Long userId) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }
        String member = userId.toString();
        List<Object> results = redisTemplate.executePipelined(
                new SessionCallback<Object>() {
                    @Override
                    public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                        for (Long postId : postIds) {
                            String key = POST_LIKE_KEY_PREFIX + postId;
                            operations.opsForZSet().score((K) key, (V) member);
                        }
                        return null;
                    }
                });

        Map<Long, Boolean> map = new HashMap<>();
        for (int i = 0; i < postIds.size(); i++) {
            map.put(postIds.get(i), results.get(i) != null);
        }
        return map;
    }

    // ===== 评论点赞 =====

    @Override
    public void likeComment(Long commentId, Long userId) {
        String key = COMMENT_LIKE_KEY_PREFIX + commentId;
        redisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
    }

    @Override
    public void unlikeComment(Long commentId, Long userId) {
        String key = COMMENT_LIKE_KEY_PREFIX + commentId;
        redisTemplate.opsForZSet().remove(key, userId.toString());
    }

    @Override
    public boolean hasLikedComment(Long commentId, Long userId) {
        String key = COMMENT_LIKE_KEY_PREFIX + commentId;
        Double score = redisTemplate.opsForZSet().score(key, userId.toString());
        return score != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Long, Boolean> hasLikedCommentsBatch(List<Long> commentIds, Long userId) {
        if (commentIds == null || commentIds.isEmpty()) {
            return Map.of();
        }
        String member = userId.toString();
        List<Object> results = redisTemplate.executePipelined(
                new SessionCallback<Object>() {
                    @Override
                    public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                        for (Long commentId : commentIds) {
                            String key = COMMENT_LIKE_KEY_PREFIX + commentId;
                            operations.opsForZSet().score((K) key, (V) member);
                        }
                        return null;
                    }
                });

        Map<Long, Boolean> map = new HashMap<>();
        for (int i = 0; i < commentIds.size(); i++) {
            map.put(commentIds.get(i), results.get(i) != null);
        }
        return map;
    }

    // ===== 浏览数 =====

    @Override
    public void incrementViewCount(Long postId) {
        redisTemplate.opsForHash().increment(VIEW_COUNT_KEY, postId.toString(), 1L);
    }

    @Override
    public Map<Long, Long> drainViewCounts() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(VIEW_COUNT_KEY);
        if (entries.isEmpty()) {
            return Map.of();
        }
        redisTemplate.delete(VIEW_COUNT_KEY);
        Map<Long, Long> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            Long postId = Long.valueOf(entry.getKey().toString());
            Long delta = Long.valueOf(entry.getValue().toString());
            result.put(postId, delta);
        }
        return result;
    }
}
