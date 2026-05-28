package com.project.demo.handler;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.demo.config.RabbitMQConfig;
import com.project.demo.dto.InterviewQAMessage;
import com.project.demo.dto.InterviewWebSocketMessage;
import com.project.demo.service.InterviewService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

@Slf4j
@Component
public class InterviewWebSocketHandler extends AbstractWebSocketHandler {

    private final InterviewService interviewService;
    private final RabbitTemplate rabbitTemplate;
    private final WebSocketSessionManager sessionManager;

    private static final String SESSION_RECORD_ID = "recordId";

    @Autowired
    private ObjectMapper objectMapper;

    public InterviewWebSocketHandler(InterviewService interviewService,
                                      RabbitTemplate rabbitTemplate,
                                      WebSocketSessionManager sessionManager) {
        this.interviewService = interviewService;
        this.rabbitTemplate = rabbitTemplate;
        this.sessionManager = sessionManager;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            log.warn("面试 WebSocket 连接失败：无法获取 userId");
            try { session.close(CloseStatus.POLICY_VIOLATION); } catch (Exception ignored) {}
            return;
        }
        log.info("面试 WebSocket 连接建立: userId={}", userId);
        sessionManager.register(userId, session);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) return;

        String payload = message.getPayload();
        InterviewWebSocketMessage wsMessage;
        try {
            wsMessage = objectMapper.readValue(payload, InterviewWebSocketMessage.class);
        } catch (Exception e) {
            log.error("消息解析失败: {}", payload, e);
            return;
        }

        String type = wsMessage.getType();
        log.info("收到控制消息: userId={}, type={}", userId, type);

        switch (type) {
            case InterviewWebSocketMessage.TYPE_START -> handleStart(session, userId);
            case InterviewWebSocketMessage.TYPE_MESSAGE ->
                handleMessage(userId, wsMessage.getContent(), wsMessage.getQuestion());
            case InterviewWebSocketMessage.TYPE_END -> handleEnd(session, userId);
            default -> log.warn("不支持的控制消息类型: {}", type);
        }
    }

    @Override
    protected void handleBinaryMessage(@NonNull WebSocketSession session, @NonNull BinaryMessage message) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) return;

        byte[] pcmAudio = message.getPayload().array();
        log.info("收到音频数据: userId={}, size={} bytes", userId, pcmAudio.length);

        Object recordIdObj = session.getAttributes().get(SESSION_RECORD_ID);
        if (recordIdObj == null) {
            log.warn("未找到面试记录，请先 START: userId={}", userId);
            return;
        }

        try {
            String generatedQuestion = interviewService.processUserAudio(userId, pcmAudio);

            Long recordId = (Long) recordIdObj;
            InterviewQAMessage qa = new InterviewQAMessage(recordId, generatedQuestion, "[语音回答]");
            rabbitTemplate.convertAndSend(RabbitMQConfig.INTERVIEW_EXCHANGE, "", JSON.toJSONString(qa));
        } catch (Exception e) {
            log.error("处理音频失败: userId={}", userId, e);
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            try {
                Object recordIdObj = session.getAttributes().get(SESSION_RECORD_ID);
                interviewService.clearInterviewMemory(userId);
                sessionManager.remove(userId);
                if (recordIdObj instanceof Long recordId) {
                    interviewService.generateFinalEvaluation(recordId);
                }
                log.info("用户资源清理完成: userId={}", userId);
            } catch (Exception e) {
                log.error("清理连接资源失败: userId={}", userId, e);
            }
        }
    }

    private void handleStart(WebSocketSession session, Long userId) {
        try {
            interviewService.clearInterviewMemory(userId);
            Long recordId = interviewService.createInterviewRecord(userId);
            session.getAttributes().put(SESSION_RECORD_ID, recordId);
            interviewService.startInterview(userId);
        } catch (Exception e) {
            log.error("开始面试失败", e);
        }
    }

    private void handleMessage(Long userId, String content, String question) {
        try {
            String generatedQuestion = interviewService.processUserText(userId, content, question);

            Object recordIdObj = sessionManager.getSession(userId)
                .getAttributes().get(SESSION_RECORD_ID);
            if (recordIdObj instanceof Long recordId) {
                InterviewQAMessage qa = new InterviewQAMessage(recordId, generatedQuestion, content);
                rabbitTemplate.convertAndSend(RabbitMQConfig.INTERVIEW_EXCHANGE, "", JSON.toJSONString(qa));
            }
        } catch (Exception e) {
            log.error("处理文字消息失败: userId={}", userId, e);
        }
    }

    private void handleEnd(WebSocketSession session, Long userId) {
        try {
            session.close(CloseStatus.NORMAL);
            log.info("面试结束，用户断开: userId={}", userId);
        } catch (Exception e) {
            log.error("断开连接失败: userId={}", userId, e);
        }
    }
}
