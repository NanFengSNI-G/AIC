package com.project.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.demo.dto.ConversationMessage;
import com.project.demo.dto.ConversationSummary;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConversationService {

    private static final String SESSIONS_KEY_PREFIX = "blog:user:";
    private static final String SESSIONS_KEY_SUFFIX = ":conversations";
    private static final String META_KEY_PREFIX = "blog:conversation:";
    private static final Duration META_TTL = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;
    private final ChatMemoryStore blogChatMemoryStore;
    private final ObjectMapper objectMapper;

    public ConversationService(StringRedisTemplate redisTemplate,
                               ChatMemoryStore blogChatMemoryStore,
                               ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.blogChatMemoryStore = blogChatMemoryStore;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建新对话，返回 sessionId。
     */
    public ConversationSummary create(Long userId, String firstMessage) {
        String sessionId = "blog-" + userId + "-" + UUID.randomUUID().toString().substring(0, 8);
        String title = firstMessage != null && !firstMessage.isBlank()
            ? (firstMessage.length() > 30 ? firstMessage.substring(0, 30) + "..." : firstMessage)
            : "新对话";
        long now = Instant.now().toEpochMilli();

        // 加入用户会话列表（ZSet，按时间排序）
        redisTemplate.opsForZSet().add(sessionsKey(userId), sessionId, now);

        // 保存会话元数据
        Map<String, Object> meta = Map.of("title", title, "createdAt", now, "updatedAt", now);
        try {
            redisTemplate.opsForValue().set(metaKey(sessionId), objectMapper.writeValueAsString(meta), META_TTL);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化会话元数据失败", e);
        }

        log.info("[Conversation] 创建对话: userId={}, sessionId={}, title={}", userId, sessionId, title);

        return ConversationSummary.builder()
            .sessionId(sessionId)
            .title(title)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    /**
     * 查询用户的历史对话列表（按最近更新时间倒序）。
     */
    public List<ConversationSummary> list(Long userId) {
        // ZSet 按 score 倒序
        Set<String> sessionIds = redisTemplate.opsForZSet()
            .reverseRange(sessionsKey(userId), 0, -1);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }

        return sessionIds.stream()
            .map(this::getMeta)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * 读取对话的聊天记录。
     */
    public List<ConversationMessage> getMessages(Long userId, String sessionId) {
        validateOwnership(userId, sessionId);

        long memoryId = memoryId(sessionId);
        List<ChatMessage> messages = blogChatMemoryStore.getMessages(memoryId);
        if (messages == null) return List.of();

        return messages.stream()
            .map(m -> {
                String role = m instanceof UserMessage ? "user" :
                              m instanceof AiMessage ? "assistant" : "system";
                return ConversationMessage.builder()
                    .role(role)
                    .content(textOf(m))
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * 删除对话：移除会话列表项 + 删除元数据 + 清除聊天记录。
     */
    public void delete(Long userId, String sessionId) {
        validateOwnership(userId, sessionId);

        redisTemplate.opsForZSet().remove(sessionsKey(userId), sessionId);
        redisTemplate.delete(metaKey(sessionId));
        blogChatMemoryStore.deleteMessages(memoryId(sessionId));

        log.info("[Conversation] 删除对话: userId={}, sessionId={}", userId, sessionId);
    }

    /**
     * 更新会话元数据（新消息时更新标题和时间）。
     */
    public void touch(Long userId, String sessionId, String message) {
        String metaJson = redisTemplate.opsForValue().get(metaKey(sessionId));
        if (metaJson == null) return;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = objectMapper.readValue(metaJson, Map.class);
            long now = Instant.now().toEpochMilli();
            meta.put("updatedAt", now);

            // 仅当标题还是 "新对话" 时用第一条消息更新标题
            if ("新对话".equals(meta.get("title")) && message != null && !message.isBlank()) {
                String title = message.length() > 30 ? message.substring(0, 30) + "..." : message;
                meta.put("title", title);
            }

            redisTemplate.opsForValue().set(metaKey(sessionId), objectMapper.writeValueAsString(meta), META_TTL);
            redisTemplate.opsForZSet().add(sessionsKey(userId), sessionId, now);
        } catch (JsonProcessingException e) {
            log.warn("[Conversation] 更新元数据失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 验证会话归属于当前用户。
     */
    public void validateOwnership(Long userId, String sessionId) {
        Double score = redisTemplate.opsForZSet().score(sessionsKey(userId), sessionId);
        if (score == null) {
            throw new SecurityException("无权操作此会话");
        }
    }

    /**
     * 从 sessionId 计算 IntentAgent 使用的 memoryId。
     */
    public static long memoryId(String sessionId) {
        return (long) sessionId.hashCode();
    }

    // ═══════════════════════════════════════════════════════════════
    // 内部方法
    // ═══════════════════════════════════════════════════════════════

    private String sessionsKey(Long userId) {
        return SESSIONS_KEY_PREFIX + userId + SESSIONS_KEY_SUFFIX;
    }

    private String metaKey(String sessionId) {
        return META_KEY_PREFIX + sessionId;
    }

    private ConversationSummary getMeta(String sessionId) {
        String json = redisTemplate.opsForValue().get(metaKey(sessionId));
        if (json == null) return null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = objectMapper.readValue(json, Map.class);
            return ConversationSummary.builder()
                .sessionId(sessionId)
                .title((String) meta.get("title"))
                .createdAt(((Number) meta.get("createdAt")).longValue())
                .updatedAt(((Number) meta.get("updatedAt")).longValue())
                .build();
        } catch (Exception e) {
            log.warn("[Conversation] 解析元数据失败: sessionId={}", sessionId, e);
            return null;
        }
    }

    private String textOf(ChatMessage m) {
        if (m instanceof UserMessage um) {
            return um.singleText();
        }
        if (m instanceof AiMessage am) {
            return am.text();
        }
        return m.toString();
    }
}
