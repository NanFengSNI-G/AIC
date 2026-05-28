package com.project.demo.service.impl;

import com.project.demo.dto.ChatMessageResponse;
import com.project.demo.dto.ConversationResponse;
import com.project.demo.entity.ChatMessage;
import com.project.demo.entity.UserDetail;
import com.project.demo.mapper.ChatMapper;
import com.project.demo.mapper.UserDetailMapper;
import com.project.demo.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatMapper chatMapper;
    private final StringRedisTemplate redisTemplate;
    private final UserDetailMapper userDetailMapper;

    private static final String CONV_KEY_PREFIX = "conv:";

    @Override
    public void updateRedisConversation(boolean inserted, Long fromUserId, Long toUserId,
                                         String content, long timestamp) {
        if (!inserted) {
            return;
        }

        String truncated = StringUtils.truncate(content, 512);
        String timeStr = String.valueOf(timestamp);
        String senderKey = CONV_KEY_PREFIX + fromUserId;
        String receiverKey = CONV_KEY_PREFIX + toUserId;

        // 5 条命令合并为 1 次网络往返
        redisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public Void execute(RedisOperations ops) {
                ops.opsForHash().put(senderKey, toUserId + ":lastMsg", truncated);
                ops.opsForHash().put(senderKey, toUserId + ":time", timeStr);
                ops.opsForHash().increment(receiverKey, fromUserId + ":unread", 1L);
                ops.opsForHash().put(receiverKey, fromUserId + ":lastMsg", truncated);
                ops.opsForHash().put(receiverKey, fromUserId + ":time", timeStr);
                return null;
            }
        });
    }

    @Override
    public List<ConversationResponse> getConversations(Long userId) {
        String key = CONV_KEY_PREFIX + userId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        // friendId → ConversationResponse builder
        Map<Long, ConversationBuilder> builders = new LinkedHashMap<>();

        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String field = entry.getKey().toString();
            String value = entry.getValue() != null ? entry.getValue().toString() : "";

            int colonIdx = field.indexOf(':');
            if (colonIdx < 0) continue;

            Long friendId = Long.parseLong(field.substring(0, colonIdx));
            String attr = field.substring(colonIdx + 1);

            builders.computeIfAbsent(friendId, f -> new ConversationBuilder(friendId));

            switch (attr) {
                case "lastMsg" -> builders.get(friendId).lastMsg = value;
                case "unread" -> builders.get(friendId).unread = Integer.parseInt(value);
                case "time" -> builders.get(friendId).time = Long.parseLong(value);
            }
        }

        if (builders.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询好友用户详情（头像、用户名）
        List<Long> friendIds = new ArrayList<>(builders.keySet());
        List<UserDetail> details = userDetailMapper.selectByUserIds(friendIds);
        Map<Long, UserDetail> detailMap = details.stream()
                .collect(Collectors.toMap(UserDetail::getUserId, d -> d));

        // 组装结果，按时间倒序
        return builders.values().stream()
                .map(b -> {
                    UserDetail detail = detailMap.get(b.friendId);
                    ConversationResponse r = new ConversationResponse();
                    r.setFriendId(b.friendId);
                    r.setLastMsg(b.lastMsg);
                    r.setUnread(b.unread);
                    r.setUpdateTime(toLocalDateTime(b.time));
                    r.setFriendUsername(detail != null ? detail.getUsername() : null);
                    r.setFriendAvatar(detail != null ? detail.getAvatar() : null);
                    return r;
                })
                .sorted((a, b) -> {
                    if (a.getUpdateTime() == null && b.getUpdateTime() == null) return 0;
                    if (a.getUpdateTime() == null) return 1;
                    if (b.getUpdateTime() == null) return -1;
                    return b.getUpdateTime().compareTo(a.getUpdateTime());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageResponse> getHistory(Long userId, Long friendId, int page, int size) {
        int offset = (page - 1) * size;
        List<ChatMessage> messages = chatMapper.selectHistory(userId, friendId, offset, size);

        return messages.stream().map(m -> {
            ChatMessageResponse r = new ChatMessageResponse();
            r.setId(m.getId());
            r.setFromUserId(m.getFromUserId());
            r.setToUserId(m.getToUserId());
            r.setContent(m.getContent());
            r.setMsgType(m.getMsgType());
            r.setCreateTime(m.getCreateTime());
            return r;
        }).collect(Collectors.toList());
    }

    @Override
    public void markAsRead(Long userId, Long friendId) {
        redisTemplate.opsForHash().put(CONV_KEY_PREFIX + userId, friendId + ":unread", "0");
    }

    @Override
    public int getUnreadTotal(Long userId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(CONV_KEY_PREFIX + userId);
        int total = 0;
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String field = entry.getKey().toString();
            if (field.endsWith(":unread")) {
                String value = entry.getValue() != null ? entry.getValue().toString() : "0";
                total += Integer.parseInt(value);
            }
        }
        return total;
    }

    private static LocalDateTime toLocalDateTime(long epochSecond) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), ZoneId.systemDefault());
    }

    /** 用于构建 ConversationResponse 的中间对象 */
    private static class ConversationBuilder {
        final Long friendId;
        String lastMsg = "";
        int unread = 0;
        long time;

        ConversationBuilder(Long friendId) {
            this.friendId = friendId;
        }
    }
}
