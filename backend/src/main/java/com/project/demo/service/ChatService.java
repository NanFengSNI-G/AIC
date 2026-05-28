package com.project.demo.service;

import com.project.demo.dto.ConversationResponse;
import com.project.demo.dto.ChatMessageResponse;

import java.util.List;

public interface ChatService {

    /** 获取最近会话列表 (Redis Hash) */
    List<ConversationResponse> getConversations(Long userId);

    /** 获取与好友的聊天历史 (分页, MySQL) */
    List<ChatMessageResponse> getHistory(Long userId, Long friendId, int page, int size);

    /** 标记与好友的所有消息已读 (清 Redis unread) */
    void markAsRead(Long userId, Long friendId);

    /** 获取未读消息总数 (Redis 汇总) */
    int getUnreadTotal(Long userId);

    /** 新消息入库后更新 Redis 会话 (仅在 inserted=true 时执行) */
    void updateRedisConversation(boolean inserted, Long fromUserId, Long toUserId, String content, long timestamp);
}
