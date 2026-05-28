package com.project.demo.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.demo.dto.InterviewWebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket Session 管理器
 * 统一管理 userId 与 session 的映射关系，供 Handler 和 Agent 工具共用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final ObjectMapper objectMapper;
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 注册 session
     */
    public void register(Long userId, WebSocketSession session) {
        sessions.put(userId, session);
        log.info("WebSocket session 注册: userId={}", userId);
    }

    /**
     * 移除 session
     */
    public void remove(Long userId) {
        sessions.remove(userId);
        log.info("WebSocket session 移除: userId={}", userId);
    }

    /**
     * 获取 session
     */
    public WebSocketSession getSession(Long userId) {
        return sessions.get(userId);
    }

    /**
     * 检查 session 是否存在且已打开
     */
    public boolean isConnected(Long userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 发送消息
     */
    public void sendMessage(Long userId, InterviewWebSocketMessage message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (Exception e) {
                log.error("发送 WebSocket 消息失败，序列化异常: userId={}", userId, e);
            }
        } else {
            log.warn("发送消息失败，session 不存在或已关闭: userId={}", userId);
        }
    }

    /**
     * 关闭 session
     */
    public void closeSession(Long userId, CloseStatus status) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.close(status);
                log.info("关闭 WebSocket session: userId={}, status={}", userId, status);
            } catch (IOException e) {
                log.error("关闭 session 失败: userId={}", userId, e);
            }
        }
    }
}