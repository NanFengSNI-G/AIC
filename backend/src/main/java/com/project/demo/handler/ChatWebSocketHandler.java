package com.project.demo.handler;

import com.alibaba.fastjson2.JSON;
import com.project.demo.config.RabbitMQConfig;
import com.project.demo.dto.ChatMessageDTO;
import com.project.demo.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 聊天处理器
 *
 * 消息流程:
 *   handleTextMessage → 对方在线? toSession.sendMessage (实时推送) : skip
 *                     → rabbitTemplate.convertAndSend (统一走 MQ)
 *                     → Consumer → insert + updateConversation (幂等)
 */
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    /** 在线用户池: userId → Session */
    public static final ConcurrentHashMap<Long, WebSocketSession> ONLINE_USERS = new ConcurrentHashMap<>();

    /** 最后一次心跳时间: userId → LocalDateTime */
    private static final ConcurrentHashMap<Long, LocalDateTime> LAST_HEARTBEAT = new ConcurrentHashMap<>();

    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${chat.heartbeat-timeout:60}")
    private int heartbeatTimeoutSeconds;

    private static final String ONLINE_USERS_SET = "online:users";

    /**
     * 连接建立成功: 标记用户上线
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            ONLINE_USERS.put(userId, session);
            LAST_HEARTBEAT.put(userId, LocalDateTime.now());

            redisTemplate.opsForSet().add(ONLINE_USERS_SET, userId);

            System.out.println("用户 " + userId + " 已连接，当前在线人数: " + ONLINE_USERS.size());
        }
    }

    /**
     * 接收消息: 解析 DTO，在线则实时推送，统一走 MQ 入库
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, @NonNull TextMessage textMessage) throws Exception {
        Long fromUserId = (Long) session.getAttributes().get("userId");
        if (fromUserId == null) {
            return;
        }

        String payload = textMessage.getPayload();

        if ("ping".equalsIgnoreCase(payload)) {
            LAST_HEARTBEAT.put(fromUserId, LocalDateTime.now());
            session.sendMessage(new TextMessage("pong"));
            return;
        }

        ChatMessageDTO dto = JSON.parseObject(textMessage.getPayload(), ChatMessageDTO.class);
        if (dto == null || dto.getToUserId() == null || !StringUtils.hasText(dto.getContent())) {
            return;
        }

        // 防止自己给自己发消息
        if (fromUserId.equals(dto.getToUserId())) {
            return;
        }

        ChatMessage msg = new ChatMessage();
        msg.setMessageId(java.util.UUID.randomUUID().toString());
        msg.setFromUserId(fromUserId);
        msg.setToUserId(dto.getToUserId());
        msg.setContent(dto.getContent());
        msg.setMsgType(dto.getMsgType() != null ? dto.getMsgType() : 1);
        msg.setCreateTime(LocalDateTime.now());

        // 对方在线: 实时推送
        WebSocketSession toSession = ONLINE_USERS.get(dto.getToUserId());
        boolean online = toSession != null && toSession.isOpen();
        if (online) {
            toSession.sendMessage(new TextMessage(JSON.toJSONString(msg)));
        }

        // 统一走 MQ 入库 (由 Consumer 负责 insert + updateConversation)
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHAT_EXCHANGE,
                RabbitMQConfig.CHAT_ROUTING_KEY,
                JSON.toJSONString(msg)
        );
    }

    /**
     * 断开连接: 移除在线状态
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            ONLINE_USERS.remove(userId);
            LAST_HEARTBEAT.remove(userId);

            redisTemplate.opsForSet().remove(ONLINE_USERS_SET, userId);

            System.out.println("用户 " + userId + " 已断开，原因: " + status + "，当前在线人数: " + ONLINE_USERS.size());
        }
    }

    /**
     * 定时检查心跳: 30秒一次
     */
    @Scheduled(fixedRate = 30000)
    public void checkHeartbeat() {
        LocalDateTime now = LocalDateTime.now();
        Iterator<Map.Entry<Long, LocalDateTime>> iterator = LAST_HEARTBEAT.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Long, LocalDateTime> entry = iterator.next();
            Long userId = entry.getKey();
            LocalDateTime lastHeartbeat = entry.getValue();

            long secondsSinceLastHeartbeat = java.time.Duration.between(lastHeartbeat, now).getSeconds();

            if (secondsSinceLastHeartbeat > heartbeatTimeoutSeconds) {
                System.out.println("用户 " + userId + " 心跳超时 (" + secondsSinceLastHeartbeat + "秒)，强制断开");

                WebSocketSession session = ONLINE_USERS.remove(userId);
                if (session != null && session.isOpen()) {
                    try {
                        session.close(CloseStatus.SESSION_NOT_RELIABLE);
                    } catch (Exception e) {
                        System.out.println("关闭超时会话失败: " + e.getMessage());
                    }
                }
                redisTemplate.opsForSet().remove(ONLINE_USERS_SET, userId);

                iterator.remove();
            }
        }
    }




}
